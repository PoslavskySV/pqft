package cc.redberry.qplatform.cluster;

import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.ModelSerdes;
import cc.redberry.qplatform.model.Theory;
import cc.redberry.qplatform.model.diagram.Diagram;
import org.apache.kafka.common.serialization.Serdes;

import static cc.redberry.qplatform.util.MemoryUnit.*;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.apache.kafka.common.config.TopicConfig.*;

/**
 *
 */
public final class KafkaTopics {
    private KafkaTopics() {}

    /** Default number of partitions used for UID/SID keyed topics */
    public static final int DEFAULT_NUM_PARTITIONS = 12;

    /** Default replication factor */
    public static final int REPLICATION_FACTOR_DEFAULT = 2;

    /** compact+delete */
    private static final String CLEANUP_POLICY_COMPACT_DELETE = CLEANUP_POLICY_COMPACT + "," + CLEANUP_POLICY_DELETE;

    /**
     * Topic to store all theories, to avoid data duplication in diagrams topic. Will be used as a backend for key-value
     * store for models.
     *
     * Diagrams store only model id.
     **/
    public static final Topic<String, Theory> Theories =
            new Topic<>("THEORIES", 1, REPLICATION_FACTOR_DEFAULT,
                    Serdes.String(), ModelSerdes.Theory)

                    // store models forever
                    .withTopicConfig(RETENTION_MS_CONFIG, -1)
                    .withTopicConfig(CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT_DELETE)
                    // allow 128 GB for raw diagrams storage
                    .withTopicConfig(RETENTION_BYTES_CONFIG, GIGABYTES.toBytes(128))

                    .withProducerConfig(ACKS_CONFIG, "all")
                    .withProducerConfig(RETRIES_CONFIG, 0)
                    .withProducerConfig(BUFFER_MEMORY_CONFIG, MEGABYTES.toBytes(64))
                    .withProducerConfig(BATCH_SIZE_CONFIG, (int) KILOBYTES.toBytes(32))
                    .withProducerConfig(LINGER_MS_CONFIG, 50)
                    .withProducerConfig(DELIVERY_TIMEOUT_MS_CONFIG, 40_000);

    /** Kafka key for theory (Theory ID) */
    public static String tid(Theory theory) {
        return theory.id;
    }

    /**
     * Topic to store all process calculation requests.
     **/
    public static final Topic<String, MatrixElementDescription> MatrixElementDescriptions =
            new Topic<>("PROCESSES", 1, REPLICATION_FACTOR_DEFAULT,
                    Serdes.String(), ModelSerdes.MatrixElementDescription)

                    // store models forever
                    .withTopicConfig(RETENTION_MS_CONFIG, -1)
                    .withTopicConfig(CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT_DELETE)
                    // allow 128 GB for raw diagrams storage
                    .withTopicConfig(RETENTION_BYTES_CONFIG, GIGABYTES.toBytes(128))

                    .withProducerConfig(ACKS_CONFIG, "all")
                    .withProducerConfig(RETRIES_CONFIG, 0)
                    .withProducerConfig(BUFFER_MEMORY_CONFIG, MEGABYTES.toBytes(64))
                    .withProducerConfig(BATCH_SIZE_CONFIG, (int) KILOBYTES.toBytes(32))
                    .withProducerConfig(LINGER_MS_CONFIG, 50)
                    .withProducerConfig(DELIVERY_TIMEOUT_MS_CONFIG, 40_000);


    /** Kafka key for matrix element (Process ID) */
    public static String pid(Theory theory, MatrixElementDescription me) {
        return tid(theory) + ":" + me.identifier();
    }

    public static final Topic<String, Diagram> RawDiagrams =
            new Topic<>("RAW_DIAGRAMS", DEFAULT_NUM_PARTITIONS, REPLICATION_FACTOR_DEFAULT,
                    Serdes.String(), ModelSerdes.Diagram)

                    // store diagrams forever
                    .withTopicConfig(RETENTION_MS_CONFIG, -1)
                    .withTopicConfig(CLEANUP_POLICY_CONFIG, CLEANUP_POLICY_COMPACT_DELETE)
                    // allow 128 GB for raw diagrams storage
                    .withTopicConfig(RETENTION_BYTES_CONFIG, GIGABYTES.toBytes(128))

                    .withProducerConfig(ACKS_CONFIG, "all")
                    .withProducerConfig(RETRIES_CONFIG, 0)
                    .withProducerConfig(BUFFER_MEMORY_CONFIG, MEGABYTES.toBytes(64))
                    .withProducerConfig(BATCH_SIZE_CONFIG, (int) KILOBYTES.toBytes(32))
                    .withProducerConfig(LINGER_MS_CONFIG, 50)
                    .withProducerConfig(DELIVERY_TIMEOUT_MS_CONFIG, 40_000);

    /** Kafka key for diagram (Diagram ID) */
    public static String did(Theory theory, MatrixElementDescription me, Diagram diag) {
        return pid(theory, me) + ":" + diag.index;
    }

    /** Diagram ID -> process ID */
    public static String did2pid(String did) {
        return did.substring(0, did.lastIndexOf(":"));
    }

    /** Process ID -> Theory ID */
    public static String pid2tid(String did) {
        return did.substring(0, did.lastIndexOf(":"));
    }

    /** Diagram ID -> Theory ID */
    public static String did2tid(String did) {
        return pid2tid(did2pid(did));
    }
}
