package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.Controller;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.RestApi;
import cc.redberry.qplatform.model.Theory;
import cc.redberry.qplatform.model.diagram.Diagram;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cc.redberry.qplatform.cluster.KafkaTopics.*;
import static cc.redberry.qplatform.endpoints.kafka.ProcessesStore.requestProcessDescription;

/**
 *
 */
public class TheoriesController implements Controller, AutoCloseable {
    public static final Logger logger = LoggerFactory.getLogger(TheoriesController.class);

    static final String prefixListTheories = "/theories/list";
    static final String prefixRun = "/run";

    private final KafkaProducer<String, Theory> theories;
    private final KafkaProducer<String, MatrixElementDescription> processes;
    private final KafkaProducer<String, Diagram> diagrams;

    public TheoriesController() {
        this.theories = Theories.createProducer();
        this.processes = KafkaTopics.MatrixElementDescriptions.createProducer();
        this.diagrams = KafkaTopics.RawDiagrams.createProducer();
    }

    private Mono<ServerResponse> listTheories(ServerRequest req) {
        logger.info("listTheories request");
        List<Theory> ths = new ArrayList<>();
        try (var consumer = Theories.createConsumer(Map.of(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.GROUP_ID_CONFIG, "default"), false)) {
            consumer.assign(IntStream.range(0, Theories.getNumPartitions()).mapToObj(i -> new TopicPartition(Theories.getFullName(), i)).collect(Collectors.toList()));
            while (true) {
                logger.info("polling theories topic");
                var recs = consumer.poll(Duration.ofMillis(100));
                if (recs.isEmpty())
                    break;

                for (ConsumerRecord<String, Theory> r : recs)
                    ths.add(r.value());
            }
        }

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Flux.fromIterable(ths), Theory.class);
    }

    private Mono<ServerResponse> run(ServerRequest req) {
        logger.info("run request");
        return req.bodyToMono(RestApi.DiagramsCalculationRequest.class)
                .zipWhen(r -> requestProcessDescription(pid(r.theory, r.description)))
                .map(r -> {
                    var th = r.getT1().theory;
                    var me = r.getT1().description;
                    var tid = tid(th);
                    var pid = pid(th, me);

                    logger.info("run for pid={}", pid);

                    var found = r.getT2();
                    if (me.equals(found)) {
                        logger.info("DiagramsCalculationRequest is duplicated pid={}", pid);
                        return "";
                    }

                    // persist in key store
                    theories.send(new ProducerRecord<>(Theories.getFullName(), tid, th));
                    processes.send(new ProducerRecord<>(MatrixElementDescriptions.getFullName(), pid, me));

                    return "";
                }).then(ServerResponse.ok().build());
    }

    @Override
    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .GET(prefixListTheories, this::listTheories)
                .PUT(prefixRun, this::run)
                //.filter(Authenticator.getAuthenticator())
                .build();
    }

    @Override
    public List<String> pathPrefixesForMonitoring() {
        return List.of(prefixListTheories, prefixRun);
    }

    public static Mono<Theory[]> listTheories() {
        return ServerUtil.requestGET(AdminServer.ENDPOINT_ADMIN, prefixListTheories, Theory[].class);
    }

    public static Mono<Void> run(RestApi.DiagramsCalculationRequest req) {
        return ServerUtil.requestPUT(AdminServer.ENDPOINT_ADMIN, prefixRun, req, Void.class);
    }

    @Override
    public void close() throws Exception {
        theories.close();
        processes.close();
        diagrams.close();
    }
}

