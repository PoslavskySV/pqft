package cc.redberry.qplatform.util;

import cc.redberry.core.context.OutputFormat;
import cc.redberry.core.tensor.Tensor;
import cc.redberry.core.tensor.Tensors;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 *
 */
public class TensorSerializers {
    public static final class TensorSerializer<T extends Tensor> extends JsonSerializer<T> {
        @Override
        public void serialize(T tensor, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(tensor == null ? null : tensor.toString(OutputFormat.Redberry));
        }
    }

    public static final class TensorDeserializer<T extends Tensor> extends JsonDeserializer<T> {
        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            String str = jsonParser.getValueAsString();
            if (str == null)
                return null;
            return (T) Tensors.parse(str);
        }
    }
}
