package cc.redberry.qplatform.endpoints.kafka;

import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theory;
import cc.redberry.qplatform.model.diagram.Diagram;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.Stores;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Mono;

/**
 *
 */
public class RawDiagramsStore {
    private RawDiagramsStore() {}

    private static final String kafkaAppName = System.getenv("KAFKA_APP_NAME");
    private static final String processesStoreName = "RawDiagramsStore";
    private static final String uriSpec = "/raw_diagrams/{id}";
    private static final String monitoringPrefix = "/raw_diagrams/";

    public static void main(String[] args) throws Exception {
        StreamsBuilder builder = new StreamsBuilder();
        builder.table(KafkaTopics.RawDiagrams.getFullName(),
                KafkaTopics.RawDiagrams.consumedWith(),
                Materialized.as(Stores.persistentKeyValueStore(processesStoreName)));

        KafkaStoreEndpoint.main(kafkaAppName, uriSpec, monitoringPrefix, processesStoreName, builder.build(), new Theory());
    }

    private static final ParameterizedTypeReference<RestApi.ValueOrDefault<Diagram>> meType = new ParameterizedTypeReference<>() {};

    /** Raw diagrams descriptions endpoint */
    private static final String ENDPOINT_RAW_DIAGRAMS = System.getenv().getOrDefault("ENDPOINT_RAW_DIAGRAMS", "http://app-endpoints-raw-diagrams:8080");

    /** Request raw diagram for specified endpoint */
    public static Mono<Diagram> requestRawDiagram(String endpoint, String did) {
        return ServerUtil.requestStore(endpoint, uriSpec, did, meType);
    }

    /** Request matrix element description */
    public static Mono<Diagram> requestRawDiagram(String did) {
        return requestRawDiagram(ENDPOINT_RAW_DIAGRAMS, did);
    }
}
