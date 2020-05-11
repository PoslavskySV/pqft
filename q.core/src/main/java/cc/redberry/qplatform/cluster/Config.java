package cc.redberry.qplatform.cluster;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.streams.StreamsConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class Config {
    public Config() {}

    public static String getEnv() {
        final String env = System.getenv("ENV");
        return env == null ? "dev" : env;
    }

    public static String getKafkaBootstrapServer() {
        return System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVER", "bootstrap.kafka.svc.cluster.local:9092");
    }

    public static String getKafkaZookeeperServer() {
        return System.getenv().getOrDefault("KAFKA_ZOOKEEPER_SERVER", "zookeeper.kafka.svc.cluster.local:2181");
    }

    public static Map<String, Object> getKafkaBaseConfig() {
        Map<String, Object> conf = new HashMap<>();
        conf.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, getKafkaBootstrapServer());
        conf.put(CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG, "5000");

        // See also:
        // https://kafka.apache.org/documentation/#topicconfigs

        return conf;
    }

    public static Map<String, Object> getKafkaAdminConfig() {
        return getKafkaBaseConfig();
    }

    public static Map<String, Object> getKafkaProducerConfig() {
        //https://kafka.apache.org/documentation/#producerconfigs
        return getKafkaBaseConfig();
    }

    public static Map<String, Object> getKafkaConsumerConfig() {
        //https://kafka.apache.org/documentation/#consumerconfigs
        return getKafkaBaseConfig();
    }

    public static Map<String, Object> getKafkaStreamingAppConfig(String baseApplicationName) {
        // https://kafka.apache.org/22/documentation/streams/developer-guide/config-streams.html
        Map<String, Object> conf = getKafkaBaseConfig();

        // See here:
        // https://kafka.apache.org/documentation/streams/developer-guide/config-streams.html

        conf.put(StreamsConfig.APPLICATION_ID_CONFIG, getKafkaApplicationName(baseApplicationName));
        conf.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, TimeUnit.SECONDS.toMillis(10));
        conf.put(StreamsConfig.STATE_DIR_CONFIG, "/kafka-state");

        return conf;
    }

    private static String getKafkaName(String baseTopicName) {
        // Bad naming convention according to:
        // https://riccomini.name/how-paint-bike-shed-kafka-topic-naming-conventions
        return "qPlatform." + getEnv() + "." + baseTopicName;
    }

    public static String getKafkaTopicName(String baseTopicName) {
        return getKafkaName(baseTopicName);
    }

    public static String getKafkaApplicationName(String baseApplicationName) {
        return getKafkaName(baseApplicationName);
    }
}
