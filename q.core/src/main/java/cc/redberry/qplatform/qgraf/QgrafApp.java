package cc.redberry.qplatform.qgraf;

import cc.redberry.qplatform.cluster.Config;
import cc.redberry.qplatform.cluster.KafkaInit;
import cc.redberry.qplatform.cluster.Topic;
import cc.redberry.qplatform.endpoints.ServerUtil;
import cc.redberry.qplatform.endpoints.kafka.TheoriesStore;
import cc.redberry.qplatform.model.MatrixElementDescription;
import cc.redberry.qplatform.model.diagram.Diagram;
import cc.redberry.qplatform.util.KafkaUtil;
import cc.redberry.qplatform.util.Util;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.netty.DisposableServer;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static cc.redberry.qplatform.cluster.KafkaTopics.*;

/**
 *
 */
public class QgrafApp {
    private static final Logger logger = LoggerFactory.getLogger(QgrafApp.class);

    public static KStream<String, Diagram>
    mkRawDiagrams(StreamsBuilder builder,
                  Topic<String, MatrixElementDescription> input) {
        return builder
                .stream(input.getFullName(), input.consumedWith())
                .flatMap((pid, proc) -> {
                    logger.info("new request pid={}", pid);
                    try {
                        // get Theory
                        var th = TheoriesStore.requestTheory(pid2tid(pid)).block();
                        var diagrams = new QgrafModel(th)
                                .generateDiagrams(proc)
                                .readAll();

                        logger.info("generated {} diagrams pid={}", pid);
                        return diagrams
                                .stream()
                                .map(d -> new KeyValue<>(did(th, proc, d), d))
                                .collect(Collectors.toList());
                    } catch (Throwable t) {
                        QgrafMetrics.get().errUnknown.increment();
                        logger.error("error computing diagrams pid={}", pid, t);
                        return Collections.emptyList();
                    }
                });
    }

    private static final String KafkaAppName =
            System.getenv().getOrDefault("KAFKA_APP_NAME", "qgraf-runner");

    @SuppressWarnings("Duplicates")
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        logger.info("Starting application (kafka app name = {})", KafkaAppName);

        logger.info("Initializing kafka topics");
        var kInit = new KafkaInit();
        kInit.createTopic(RawDiagrams);
        kInit.execute();

        logger.info("Starting kafka app");

        StreamsBuilder builder = new StreamsBuilder();
        var results = mkRawDiagrams(builder, MatrixElementDescriptions);
        results.to(RawDiagrams.getFullName(), RawDiagrams.producedWith());

        var topology = builder.build();
        var config = Config.getKafkaStreamingAppConfig(KafkaAppName);
        try (var streams = new KafkaStreams(topology, Util.map2prop(config))) {
            CountDownLatch latch = new CountDownLatch(1);
            streams.setUncaughtExceptionHandler((t, e) -> {
                logger.error("Error in kafka app", e);
                latch.countDown();
            });

            streams.cleanUp();
            streams.start();

            KafkaUtil.waitKafkaAppRunning(streams);

            // run metrics server and block
            DisposableServer server = ServerUtil.runServer("qgraf", false, true);

            latch.await();
            logger.warn("shutting down");
            server.disposeNow(Duration.ofSeconds(3));
        }
    }
}
