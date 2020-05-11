package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.qplatform.endpoints.Controller;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public final class PingController implements Controller {
    public PingController() {}

    public Mono<ServerResponse> pong(ServerRequest req) {
        return ServerResponse
                .ok()
                .contentType(MediaType.TEXT_PLAIN)
                .syncBody("pong");
    }

    @Override
    public List<String> pathPrefixesForMonitoring() {
        return Collections.singletonList("/ping");
    }

    @Override
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET("/ping", this::pong)
                .build();
    }
}
