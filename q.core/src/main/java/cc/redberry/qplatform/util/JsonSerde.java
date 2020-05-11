package cc.redberry.qplatform.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Json Serder for Kafka.
 */
public final class JsonSerde<T> implements Serde<T> {
    private final Class<T> clazz;
    private final TypeReference<T> typeReference;
    private final Serializer<T> ser;
    private final Deserializer<T> der;
    private final Consumer<T> validate;

    public JsonSerde(Class<T> clazz, Consumer<T> validate) {
        this.clazz = clazz;
        this.typeReference = null;
        this.ser = new Ser();
        this.der = new Der();
        this.validate = validate;
    }

    public JsonSerde(TypeReference<T> typeReference, Consumer<T> validate) {
        this.clazz = null;
        this.typeReference = typeReference;
        this.ser = new Ser();
        this.der = new Der();
        this.validate = validate;
    }

    public JsonSerde(Class<T> clazz) {
        this(clazz, null);
    }

    public JsonSerde(TypeReference<T> typeReference) {
        this(typeReference, null);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public void close() {}

    @Override
    public Serializer<T> serializer() {
        return ser;
    }

    @Override
    public Deserializer<T> deserializer() {
        return der;
    }

    private final class Ser implements Serializer<T> {
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) { }

        @Override
        public byte[] serialize(String topic, T data) {
            try {
                if (clazz != null)
                    return JsonUtil.JsonMapper.writerFor(clazz).writeValueAsBytes(data);
                else
                    return JsonUtil.JsonMapper.writerFor(typeReference).writeValueAsBytes(data);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}
    }

    private final class Der implements Deserializer<T> {
        @Override
        public void configure(Map<String, ?> configs, boolean isKey) { }

        @Override
        public T deserialize(String topic, byte[] data) {
            try {
                T r;
                if (clazz != null)
                    r = JsonUtil.JsonMapper.readValue(data, clazz);
                else
                    r = JsonUtil.JsonMapper.readValue(data, typeReference);
                if (validate != null)
                    validate.accept(r);
                return r;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}
    }
}
