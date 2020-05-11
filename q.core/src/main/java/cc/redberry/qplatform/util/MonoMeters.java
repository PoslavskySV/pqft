package cc.redberry.qplatform.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** wrapper around Reactor's mono metrics */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.NONE)
public final class MonoMeters  {
    @JsonProperty("name")
    public final String name;

    public MonoMeters(String name) {
        this.name = name;
    }

    private RequiredSearch selector() {
        return io.micrometer.core.instrument.Metrics.globalRegistry
                .get("reactor.flow.duration")
                .tag("flow", name);
    }

    @JsonIgnore
    private Map<String, Timer> _timers = new HashMap<>();

    public synchronized Timer timer(String status) {
        Timer timer = _timers.get(status);
        if (timer != null)
            return timer;
        Collection<Timer> c;
        try {
            c = selector().tag("status", status).timers();
        } catch (MeterNotFoundException e) {
            c = Collections.emptyList();
        }
        if (c.isEmpty())
            return null;
        return _timers.put(status, c.iterator().next());
    }

    public long count(String status) {
        Timer t = timer(status);
        if (t == null)
            return -1;
        return t.count();
    }

    public long mean(String status) {
        Timer t = timer(status);
        if (t == null) return -1;
        return (long) t.mean(TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("c#").append(count("completed")).append("$").append(mean("completed"));
        sb.append(",");
        sb.append("e#").append(count("error")).append("$").append(mean("error"));
        return sb.toString();
    }

    @JsonProperty
    @JsonSerialize(using = MicrometerUtil.TimerSerializer.class)
    public Timer complete() {
        return timer("completed");
    }

    @JsonProperty
    @JsonSerialize(using = MicrometerUtil.TimerSerializer.class)
    public Timer error() {
        return timer("error");
    }
}
