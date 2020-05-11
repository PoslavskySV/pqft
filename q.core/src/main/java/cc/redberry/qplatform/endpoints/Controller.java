package cc.redberry.qplatform.endpoints;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface Controller {
    RouterFunction<ServerResponse> routes();

    List<String> pathPrefixesForMonitoring();

    static Set<String> allPrefixes(Controller... controllers) {
        return Arrays
                .stream(controllers)
                .flatMap(c -> c.pathPrefixesForMonitoring().stream())
                .collect(Collectors.toSet());

    }

    static RouterFunction<ServerResponse> route(Controller... controllers) {
        return Arrays
                .stream(controllers)
                .map(Controller::routes)
                .reduce(RouterFunction::and)
                .get();
    }
}
