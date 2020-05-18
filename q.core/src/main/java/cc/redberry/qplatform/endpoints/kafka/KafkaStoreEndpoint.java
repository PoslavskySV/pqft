package cc.redberry.qplatform.endpoints.kafka;


import cc.redberry.qplatform.cluster.Config;
import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.Authenticator;
import cc.redberry.qplatform.endpoints.Controller;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.model.RestApi.ValueOrDefault;
import cc.redberry.qplatform.util.KafkaUtil;
import cc.redberry.qplatform.util.Util;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static cc.redberry.qplatform.endpoints.ServerUtil.*;

/**
 *
 */
final class KafkaStoreEndpoint<V> implements Controller {
    private static final Logger logger = LoggerFactory.getLogger(KafkaStoreEndpoint.class);
    /** self host info */
    private final HostInfo hostInfo;
    /** app name */
    private final String appName;
    /** forward counter */
    public final Counter
            requestsNotForwardedCounter,
            requestsForwardedCounter,
            forwardCounter;
    public final Timer
            storeRetrieveTime;
    /** uri */
    private final String uriSpec;
    /** monitoring prefix */
    private final String prefix;
    /** kafka streams instance */
    private final KafkaStreams streams;
    /** persistent store name */
    private final String storeName;
    /** persistent uid -> data store */
    private final ReadOnlyKeyValueStore<String, V> store;
    /** default data */
    private final V defaultInstance;

    KafkaStoreEndpoint(String appName, HostInfo hostInfo, String uriSpec, String monitoringPrefix, KafkaStreams streams, String storeName, V defaultInstance) {
        this.appName = appName;
        this.hostInfo = hostInfo;
        this.uriSpec = uriSpec;
        this.prefix = monitoringPrefix;
        this.streams = streams;
        this.storeName = storeName;
        this.store = streams.store(storeName, QueryableStoreTypes.keyValueStore());
        this.defaultInstance = defaultInstance;
        this.requestsNotForwardedCounter = Counter
                .builder("kafka.endpoint.request")
                .tags("app", appName, "forwarded", "no")
                .description("Number of requests")
                .register(Metrics.globalRegistry);
        this.requestsForwardedCounter = Counter
                .builder("kafka.endpoint.request")
                .tags("app", appName, "forwarded", "yes")
                .description("Number of requests")
                .register(Metrics.globalRegistry);
        this.forwardCounter = Counter
                .builder("kafka.endpoint.forwards")
                .tags("app", appName)
                .description("Number of request forwards to other node")
                .register(Metrics.globalRegistry);
        this.storeRetrieveTime = Timer
                .builder("kafka.endpoint.store.retrieve")
                .tags("app", appName)
                .description("Time spent retrieving data from Kafka store")
                .register(Metrics.globalRegistry);
    }

    /** Retrieve data from Kafka store */
    Mono<ServerResponse> getData(ServerRequest req) {
        boolean forward = req.queryParam("forward").isPresent();

        if (forward)
            requestsForwardedCounter.increment();
        else
            requestsNotForwardedCounter.increment();

        String id = req.pathVariable("id");
        logger.debug("request id = {}, forward = {}", id, forward);

        var metadata = streams.metadataForKey(storeName, id, Serdes.String().serializer());

        logger.trace("request id = {}, metadata = {}", id, metadata);
        if (StreamsMetadata.NOT_AVAILABLE.equals(metadata)) {
            logger.error("received StreamsMetadata.NOT_AVAILABLE on request id = {}", id);
            return mk(defaultInstance, true);
        }

        if (metadata == null) {
            logger.error("received null metadata on request id = {}", id);
            return mk(defaultInstance, true);
        }

        var hostInfo = metadata.hostInfo();
        logger.debug("request id = {}, self = {}:{}, holder = {}:{}", id, this.hostInfo.host(), this.hostInfo.port(), hostInfo.host(), hostInfo.port());
        if (hostInfo.host().equals(this.hostInfo.host())) {
            long start = System.nanoTime();
            V val = store.get(id);
            storeRetrieveTime.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
            if (val != null) {
                logger.debug("request id = {}, found value in local store", id);
                // found data in the local store
                return mk(val, false);
            } else {
                logger.info("request id = {}, value can't be found in the local store",
                        id, hostInfo);
                return mk(defaultInstance, true);
            }
        }

        if (forward) {
            logger.error("recursive forwarding, id = {}", id);
            return mk(defaultInstance, true);
        }

        forwardCounter.increment();

        logger.debug("forwarding to host = {}:{}, id = {}", hostInfo.host(), hostInfo.port(), id);
        URI uri = req.uriBuilder().host(hostInfo.host()).port(hostInfo.port()).queryParam("forward", 1).build();
        return WebClient.create()
                .get()
                .uri(uri)
                .header(HEADER_X_AUTH, req.headers().header(HEADER_X_AUTH).toArray(String[]::new))
                .exchange()
                .flatMap(mapper -> ServerResponse
                        .status(mapper.rawStatusCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(mapper.bodyToFlux(DataBuffer.class), DataBuffer.class));
    }

    @Override
    public List<String> pathPrefixesForMonitoring() {
        return Collections.singletonList(prefix);
    }

    @Override
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET(uriSpec, this::getData)
                .filter(Authenticator.getAuthenticator())
                .build();
    }

    private static <V> Mono<ServerResponse> mk(V v, boolean isDefault) {
        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .syncBody(new ValueOrDefault<>(v, isDefault));
    }

    /** app main */
    static <T> void main(String kafkaAppName,
                         String uriSpec,
                         String monitoringPrefix,
                         String storeName,
                         Topology topology,
                         T defaultInstance) throws Exception {
        KafkaTopics.initAll();

        Objects.requireNonNull(kafkaAppName, "kafkaAppName");

        var config = Config.getKafkaStreamingAppConfig(kafkaAppName);
        config.put(StreamsConfig.APPLICATION_SERVER_CONFIG, IpAddr + ":" + HttpPort);

        KafkaUtil.startKafkaStateFolderMonitoring(kafkaAppName);

        var latch = new CountDownLatch(1);
        try (var streams = new KafkaStreams(topology, Util.map2prop(config))) {
            streams.setUncaughtExceptionHandler((t, e) -> {
                logger.error("Error in kafka app, exiting", e);
                latch.countDown();
            });
            streams.cleanUp();
            streams.start();

            KafkaUtil.waitKafkaAppRunning(streams);

            var endpoint = new KafkaStoreEndpoint<>(kafkaAppName, ServerUtil.HostInfo, uriSpec, monitoringPrefix, streams, storeName, defaultInstance);
            var server = ServerUtil.runServer(kafkaAppName, false, true, endpoint);
            latch.await();
            server.disposeNow(Duration.ofSeconds(3));
        } finally {
            logger.error("exiting");
            System.exit(1);
        }
    }
}
