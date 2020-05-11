package cc.redberry.qplatform.cluster;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

public final class Topic<K, V> {
    private final String name;
    private final int numPartitions;
    private final short replicationFactor;
    private final Serde<K> keySerde;
    private final Serde<V> valueSerde;
    private final Map<String, String> topicConfig;
    private final Map<String, Object> producerConfig;
    private final Map<String, Object> consumerConfig;

    public Topic(String name, int numPartitions, short replicationFactor, Serde<K> keySerde, Serde<V> valueSerde,
                 Map<String, String> topicConfig, Map<String, Object> producerConfig, Map<String, Object> consumerConfig) {
        this.name = name;
        this.numPartitions = numPartitions;
        this.replicationFactor = replicationFactor;
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
        this.topicConfig = topicConfig;
        this.producerConfig = producerConfig;
        this.consumerConfig = consumerConfig;
    }

    public Topic(String name, int numPartitions, int replicationFactor,
                 Serde<K> keySerde, Serde<V> valueSerde) {
        //noinspection unchecked
        this(name, numPartitions, (short) replicationFactor,
                keySerde, valueSerde,
                Collections.EMPTY_MAP,
                Collections.EMPTY_MAP,
                Collections.EMPTY_MAP);
    }

    private String getName() {
        return name;
    }

    public String getFullName() {
        return Config.getKafkaTopicName(name);
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public short getReplicationFactor() {
        return replicationFactor;
    }

    public Map<String, String> getTopicConfig() {
        return topicConfig;
    }

    public long getRetentionMs() {
        String ms = topicConfig.get(RETENTION_MS_CONFIG);
        if (ms == null)
            return -1;
        return Long.parseLong(ms);
    }

    public Serde<K> getKeySerde() {
        return keySerde;
    }

    public Serde<V> getValueSerde() {
        return valueSerde;
    }

    public Topic<K, V> withName(String newName) {
        return new Topic<>(newName, numPartitions, replicationFactor,
                keySerde, valueSerde,
                topicConfig, producerConfig, consumerConfig);
    }

    /**
     * https://kafka.apache.org/documentation/#topicconfigs
     */
    public Topic<K, V> withTopicConfig(String key, String value) {
        HashMap<String, String> newConfig = new HashMap<>(topicConfig);
        newConfig.put(key, value);
        return new Topic<>(name, numPartitions, replicationFactor,
                keySerde, valueSerde,
                Collections.unmodifiableMap(newConfig),
                producerConfig,
                consumerConfig);
    }

    /**
     * https://kafka.apache.org/documentation/#topicconfigs
     */
    public Topic<K, V> withTopicConfig(String key, long value) {
        return withTopicConfig(key, Long.toString(value));
    }

    public Topic<K, V> withoutTopicConfig(String key) {
        HashMap<String, String> newConfig = new HashMap<>(topicConfig);
        newConfig.remove(key);
        return new Topic<>(name, numPartitions, replicationFactor,
                keySerde, valueSerde,
                Collections.unmodifiableMap(newConfig),
                producerConfig,
                consumerConfig);
    }

    /**
     * https://kafka.apache.org/documentation/#producerconfigs
     */
    public Topic<K, V> withProducerConfig(String key, Object value) {
        HashMap<String, Object> newConfig = new HashMap<>(producerConfig);
        newConfig.put(key, value);
        return new Topic<>(name, numPartitions, replicationFactor,
                keySerde, valueSerde,
                topicConfig,
                Collections.unmodifiableMap(newConfig),
                consumerConfig);
    }

    /**
     * https://kafka.apache.org/documentation/#consumerconfigs
     */
    public Topic<K, V> withConsumerConfig(String key, Object value) {
        HashMap<String, Object> newConfig = new HashMap<>(consumerConfig);
        newConfig.put(key, value);
        return new Topic<>(name, numPartitions, replicationFactor,
                keySerde, valueSerde,
                topicConfig,
                producerConfig,
                Collections.unmodifiableMap(newConfig));
    }

    /**
     * Convert to Kafka's Admin NewTopic
     */
    public NewTopic toNewTopic() {
        return new NewTopic(getFullName(), numPartitions, replicationFactor).configs(topicConfig);
    }

    public Consumed<K, V> consumedWith() {
        return Consumed.with(keySerde, valueSerde);
    }

    public Produced<K, V> producedWith() {
        return Produced.with(keySerde, valueSerde);
    }

    private static Map<String, Object> override(Map<String, Object> original, Map<String, Object>... overrides) {
        Map<String, Object> props = new HashMap<>(original);
        for (Map<String, Object> override : overrides)
            props.putAll(override);
        return props;
    }

    public KafkaProducer<K, V> createProducer() {
        //noinspection unchecked
        return createProducer(Collections.EMPTY_MAP);
    }

    public KafkaProducer<K, V> createProducer(Map<String, Object> configsOverrides) {
        //noinspection unchecked
        return new KafkaProducer<>(
                override(Config.getKafkaProducerConfig(), producerConfig, configsOverrides),
                keySerde.serializer(), valueSerde.serializer());
    }

    public KafkaConsumer<K, V> createConsumer(Map<String, Object> configsOverrides, boolean subscribe) {
        //noinspection unchecked
        KafkaConsumer<K, V> consumer = new KafkaConsumer<>(
                override(Config.getKafkaConsumerConfig(), consumerConfig, configsOverrides),
                keySerde.deserializer(), valueSerde.deserializer());
        if (subscribe)
            consumer.subscribe(Collections.singleton(getFullName()));
        return consumer;
    }

    public KafkaConsumer<K, V> createConsumer(String consumerGroupId, boolean subscribe) {
        return createConsumer(new HashMap<>() {{
            put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        }}, subscribe);
    }

    public KafkaConsumer<K, V> createConsumer(String consumerGroupId) {
        return createConsumer(consumerGroupId, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Topic)) return false;
        Topic topic = (Topic) o;
        return numPartitions == topic.numPartitions &&
                replicationFactor == topic.replicationFactor &&
                name.equals(topic.name) &&
                topicConfig.equals(topic.topicConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, numPartitions, replicationFactor, topicConfig);
    }
}
