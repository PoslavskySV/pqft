package cc.redberry.qplatform.endpoints;

import cc.redberry.qplatform.util.MonitoringUtil;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;


/**
 *
 */
public class PrometheusController implements Controller {
    private final PrometheusMeterRegistry registry;

    public PrometheusController() {
        MonitoringUtil.ensureMonitoringInitialized();

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

        // Registering this registry as global, so it will listen to all the metrics
        // created through the global composite registry
        Metrics.addRegistry(registry);

        this.registry = registry;
    }

    public Mono<ServerResponse> scrape(ServerRequest req) {
        return ServerResponse.ok().contentType(MediaType.TEXT_PLAIN).syncBody(registry.scrape());
    }

    @Override
    public List<String> pathPrefixesForMonitoring() {
        return Collections.singletonList("/metrics");
    }

    @Override
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET("/metrics", this::scrape)
                .build();
    }
}
