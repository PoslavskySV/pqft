package cc.redberry.qplatform.endpoints;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.reactivestreams.Publisher;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class MetricFilter implements WebFilter {
    private final MeterRegistry registry;
    private final String appName;
    private final ConcurrentMap<PathCode, Timer> timers = new ConcurrentHashMap<>();
    private final List<String> prefixes = new ArrayList<>();

    public MetricFilter(String appName) {
        this(Metrics.globalRegistry, appName);
    }

    public MetricFilter(MeterRegistry registry, String appName) {
        this.registry = registry;
        this.appName = appName;
    }

    public MetricFilter trackAddressPrefixes(Collection<String> prefixes) {
        for (String prefix : prefixes)
            trackAddressPrefix(prefix);
        return this;
    }

    public MetricFilter trackAddressPrefix(String prefix) {
        prefixes.add(prefix);
        return this;
    }

    private String resolvePrefix(String path) {
        for (String prefix : prefixes)
            if (path.startsWith(prefix))
                return prefix;
        return "unknown";
    }

    public Timer getTimer(PathCode pathCode) {
        return timers.computeIfAbsent(pathCode, pc ->
                Timer.builder("q.endpoint")
                        .publishPercentiles(0.5, 0.9, 0.99, 0.999)
                        .description("Time spent serving the request")
                        .tags(
                                "app", appName,
                                "uri", pc.prefix,
                                "status.code", "" + pathCode.statusCode)
                        .register(registry));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange).transformDeferred(call -> filter(exchange, call));
    }

    private Publisher<Void> filter(ServerWebExchange exchange, Mono<Void> call) {
        long start = System.nanoTime();
        return call.doOnSuccess((done) -> onSuccess(exchange, start))
                .doOnError((cause) -> onError(exchange, start, cause));
    }

    private void onSuccess(ServerWebExchange exchange, long start) {
        record(exchange, start, null);
    }

    private void onError(ServerWebExchange exchange, long start, Throwable cause) {
        ServerHttpResponse response = exchange.getResponse();
        if (response.isCommitted()) {
            record(exchange, start, cause);
        } else
            response.beforeCommit(() -> {
                record(exchange, start, cause);
                return Mono.empty();
            });
    }

    private void record(ServerWebExchange exchange, long start, Throwable cause) {
        String pathPrefix = resolvePrefix(exchange.getRequest().getPath().toString());
        PathCode pc = new PathCode(pathPrefix, exchange.getResponse().getStatusCode().value());
        getTimer(pc).record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
    }

    public final static class PathCode {
        final String prefix;
        final int statusCode;

        public PathCode(String prefix, int statusCode) {
            this.prefix = prefix;
            this.statusCode = statusCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PathCode)) return false;
            PathCode pathCode = (PathCode) o;
            return statusCode == pathCode.statusCode &&
                    prefix.equals(pathCode.prefix);
        }

        @Override
        public int hashCode() {
            return Objects.hash(prefix, statusCode);
        }
    }
}
