package cc.redberry.qplatform.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Timer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 *
 */
public class MicrometerUtil {
    public static class CounterSerializer extends JsonSerializer<Counter> {
        @Override
        public void serialize(Counter c, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("count", c.count());
            gen.writeEndObject();
        }
    }

    public static class TimerSerializer extends JsonSerializer<Timer> {
        @Override
        public void serialize(Timer t, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("count", t.count());
            gen.writeObjectField("mean", t.mean(TimeUnit.MILLISECONDS) + "ms");
            gen.writeObjectField("total", t.totalTime(TimeUnit.MILLISECONDS) + "ms");
            gen.writeEndObject();
        }
    }

    public static class DistributionSummarySerializer extends JsonSerializer<DistributionSummary> {
        @Override
        public void serialize(DistributionSummary d, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();
            gen.writeObjectField("count", d.count());
            gen.writeObjectField("mean", d.mean());
            gen.writeObjectField("max", d.max());
            gen.writeEndObject();
        }
    }
}
