package cc.redberry.qplatform.endpoints.kafka;

import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theory;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

/**
 *
 */
public final class TheoriesStore {
    private TheoriesStore() {}

    private static final String kafkaAppName = System.getenv("KAFKA_APP_NAME");
    private static final String theoriesStoreName = "TheoriesStore";
    private static final String uriSpec = "/theories/{id}";
    private static final String monitoringPrefix = "/theories/";

    public static void main(String[] args) throws Exception {
        StreamsBuilder builder = new StreamsBuilder();
        builder.table(KafkaTopics.Theories.getFullName(),
                KafkaTopics.Theories.consumedWith(),
                Materialized.as(Stores.persistentKeyValueStore(theoriesStoreName)));

        KafkaStoreEndpoint.main(kafkaAppName, uriSpec, monitoringPrefix, theoriesStoreName, builder.build(), new Theory());
    }

    private static final ParameterizedTypeReference<RestApi.ValueOrDefault<Theory>> theoryType = new ParameterizedTypeReference<>() {};

    /** Theories endpoint */
    private static final String ENDPOINT_THEORIES = System.getenv().getOrDefault("ENDPOINT_THEORIES", "http://app-endpoints-theories:8080");

    /** Request theory */
    public static Mono<Theory> requestTheory(String endpoint, String tid) {
        return ServerUtil.requestStore(endpoint, uriSpec, tid, theoryType);
    }

    /** Request theory */
    public static Mono<Theory> requestTheory(String tid) {
        return requestTheory(ENDPOINT_THEORIES, tid);
    }
}
