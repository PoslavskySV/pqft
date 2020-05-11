package cc.redberry.qplatform.cluster;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.TopicExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

// On the topic:
// https://riccomini.name/managing-kafka-topic-configuration
public final class KafkaInit {
    private static final Logger log = LoggerFactory.getLogger(KafkaInit.class);

    private final ArrayList<Topic> topics = new ArrayList<>();

    /**
     * @param topic topic settings
     * @return topic with actual topic name
     */
    public <K, V> void createTopic(Topic<K, V> topic) {
        topics.add(topic);
    }

    public void execute() throws ExecutionException {
        try (AdminClient adminClient = AdminClient.create(Config.getKafkaAdminConfig())) {
            // Initiating topic creation
            final CreateTopicsResult result = adminClient.createTopics(this.topics.stream()
                    .map(Topic::toNewTopic)
                    .collect(Collectors.toList()));

            // Waiting for operation to complete
            for (Map.Entry<String, KafkaFuture<Void>> entry : result.values().entrySet()) {
                final KafkaFuture<Void> future = entry.getValue();
                final String topicName = entry.getKey();

                boolean ok = true;
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    ok = false;
                    if (e.getCause() instanceof TopicExistsException)
                        log.info("Topic '{}' already exist", topicName);
                    else {
                        log.error("Error creating topic '{}'", topicName, e.getCause());
                        throw new RuntimeException(e.getCause());
                    }
                }

                if (ok)
                    log.info("Topic '{}' created successfully", topicName);
            }
        }
    }
}
