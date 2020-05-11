package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.Controller;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theory;
import cc.redberry.qplatform.model.diagram.Diagram;
import cc.redberry.qplatform.qgraf.QgrafModel;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.List;

import static cc.redberry.qplatform.cluster.KafkaTopics.*;

/** Generates diagrams for specified process */
public class QgrafController implements Controller, AutoCloseable {
    static final String prefix = "/qgraf/";

    private final KafkaProducer<String, Theory> theories;
    private final KafkaProducer<String, MatrixElementDescription> processes;
    private final KafkaProducer<String, Diagram> diagrams;

    public QgrafController() {
        this.theories = Theories.createProducer();
        this.processes = KafkaTopics.MatrixElementDescriptions.createProducer();
        this.diagrams = KafkaTopics.RawDiagrams.createProducer();
    }

    private Mono<ServerResponse> generateDiagrams(ServerRequest req) {
        return req.bodyToMono(RestApi.DiagramsCalculationRequest.class).map(r -> {
            var theory = r.theory;
            var process = r.description;

            // persist in key store
            theories.send(new ProducerRecord<>(Theories.getFullName(), tid(theory), theory));
            processes.send(new ProducerRecord<>(MatrixElementDescriptions.getFullName(), pid(theory, process), process));

            try (var reader = new QgrafModel(theory).generateDiagrams(process)) {
                Diagram diagram;
                while ((diagram = reader.next()) != null)
                    diagrams.send(new ProducerRecord<>(RawDiagrams.getFullName(), did(theory, process, diagram), diagram));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return "";
        }).then(ServerResponse.ok().build());
    }

    @Override
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .PUT(prefix, RequestPredicates.contentType(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON_UTF8),
                        this::generateDiagrams)
                //.filter(Authenticator.getAuthenticator())
                .build();
    }

    @Override
    public List<String> pathPrefixesForMonitoring() {
        return List.of(prefix);
    }

    @Override
    public void close() throws Exception {
        theories.close();
        processes.close();
        diagrams.close();
    }
}
