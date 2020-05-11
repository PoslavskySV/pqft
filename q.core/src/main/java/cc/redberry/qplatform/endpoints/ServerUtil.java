package cc.redberry.qplatform.endpoints;

import cc.redberry.qplatform.model.RestApi.ValueOrDefault;
import org.apache.kafka.streams.state.HostInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;

/**
 *
 */
public final class ServerUtil {
    private static final Logger logger = LoggerFactory.getLogger(ServerUtil.class);

    private ServerUtil() {}

    /**
     * https://github.com/ndjson/ndjson-spec https://github.com/wardi/jsonlines/issues/9#issuecomment-486229101
     */
    public static MediaType APPLICATION_NDJSON = MediaType.valueOf("application/x-ndjson");

    public static String HEADER_X_AUTH = "X-Auth";

    /** ip address of this machine */
    public static final String IpAddr;
    /** exposed port */
    public static final int HttpPort;
    /** host info */
    public static final HostInfo HostInfo;

    static {
        try {
            IpAddr = System.getenv().getOrDefault("HOST_IP", InetAddress.getLocalHost().getHostAddress());
            HttpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8080"));
            HostInfo = new HostInfo(IpAddr, HttpPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException();
        }
    }

    /** run Spring server and block */
    public static DisposableServer runServer(String appName, boolean block, boolean exposeMetrics, Controller... cnts) {
        if (exposeMetrics) {
            cnts = Arrays.copyOf(cnts, cnts.length + 1);
            cnts[cnts.length - 1] = new PrometheusController();
        }

        return runServer(appName, block, Controller.allPrefixes(cnts), Controller.route(cnts));
    }

    /** run Spring server and block */
    public static DisposableServer runServer(String appName, boolean block, Set<String> monitoringPrefixes, RouterFunction<ServerResponse> routeFunc) {
        // -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false
        System.setProperty("reactor.netty.http.server.accessLogEnabled", "true");
        var httpServer = HttpServer.create().host("0.0.0.0").port(HttpPort);
        var adapter = new ReactorHttpHandlerAdapter(RouterFunctions.toHttpHandler(routeFunc,
                HandlerStrategies.builder()
                        .webFilter(
                                new MetricFilter(appName)
                                        .trackAddressPrefixes(monitoringPrefixes)
                        )
                        .build()));
        httpServer = httpServer.handle(adapter);

        if (block) {
            httpServer.bindUntilJavaShutdown(Duration.ofSeconds(3), disposableServer -> {
                LoggerFactory.getLogger(Controller.class).info("Started.");
            });
            logger.warn("server stopped due to JVM shutdown");
            return null;
        } else
            return httpServer.bindNow(Duration.ofSeconds(3));
    }

    /** Util method to request data from endpoint */
    public static <T> Mono<T> requestGET(String baseUrl, String uri, Class<T> clazz) {
        return WebClient.create(baseUrl)
                .get()
                .uri(uri)
                .header(HEADER_X_AUTH, Authenticator.getAuthenticator().getXAuthToken())
                .retrieve()
                .bodyToMono(clazz)
                .doOnError(err -> logger.warn("error while fetching http://{}{}", baseUrl, uri, err));
    }

    /** Util method to request data from endpoint */
    public static <T, B> Mono<T> requestPUT(String baseUrl, String uri, B body, Class<T> clazz) {
        return WebClient.create(baseUrl)
                .put()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .header(HEADER_X_AUTH, Authenticator.getAuthenticator().getXAuthToken())
                .retrieve()
                .bodyToMono(clazz)
                .doOnError(err -> logger.warn("error while fetching http://{}{}", baseUrl, uri, err));
    }


    /** Util method to request data from endpoint */
    public static <T> Mono<T> requestStore(String baseUrl, String uriSpec, String uid, ParameterizedTypeReference<ValueOrDefault<T>> clazz) {
        return WebClient.create(baseUrl)
                .get()
                .uri(uriSpec, uid)
                .header(HEADER_X_AUTH, Authenticator.getAuthenticator().getXAuthToken())
                .retrieve()
                .bodyToMono(clazz)
                .map(t -> t.value)
                .doOnError(err -> logger.warn("error while fetching http://{}{}", baseUrl, uriSpec, err));
    }
}
