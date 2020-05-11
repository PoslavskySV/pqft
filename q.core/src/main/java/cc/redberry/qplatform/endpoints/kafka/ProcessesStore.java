package cc.redberry.qplatform.endpoints.kafka;

import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.model.MatrixElementDescription;
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
public class ProcessesStore {
    private ProcessesStore() {}

    private static final String kafkaAppName = System.getenv("KAFKA_APP_NAME");
    private static final String processesStoreName = "ProcessesStore";
    private static final String uriSpec = "/processes/{id}";
    private static final String monitoringPrefix = "/processes/";

    public static void main(String[] args) throws Exception {
        StreamsBuilder builder = new StreamsBuilder();
        builder.table(KafkaTopics.MatrixElementDescriptions.getFullName(),
                KafkaTopics.MatrixElementDescriptions.consumedWith(),
                Materialized.as(Stores.persistentKeyValueStore(processesStoreName)));

        KafkaStoreEndpoint.main(kafkaAppName, uriSpec, monitoringPrefix, processesStoreName, builder.build(), new Theory());
    }

    private static final ParameterizedTypeReference<RestApi.ValueOrDefault<MatrixElementDescription>> meType = new ParameterizedTypeReference<>() {};

    /** Matrix element descriptions endpoint */
    private static final String ENDPOINT_PROCESSES = System.getenv().getOrDefault("ENDPOINT_PROCESSES", "http://app-endpoints-processes:8080");

    /** Request matrix element description */
    public static Mono<MatrixElementDescription> requestProcessDescription(String endpoint, String pid) {
        return ServerUtil.requestStore(endpoint, uriSpec, pid, meType);
    }

    /** Request matrix element description */
    public static Mono<MatrixElementDescription> requestProcessDescription(String pid) {
        return requestProcessDescription(ENDPOINT_PROCESSES, pid);
    }
}
