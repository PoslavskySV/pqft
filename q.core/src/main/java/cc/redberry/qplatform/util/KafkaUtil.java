package cc.redberry.qplatform.util;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 *
 */
public final class KafkaUtil {
    private static final Logger logger = LoggerFactory.getLogger(KafkaUtil.class);

    private KafkaUtil() {}

    public static <K, V> Mono<RecordMetadata> send(KafkaProducer<K, V> producer, ProducerRecord<K, V> record) {
        return Mono.create(sink -> {
            // long start = System.nanoTime();
            // Sending kafka message, and saving the future to make operation cancel possible
            Future<RecordMetadata> sendFuture = producer.send(record,
                    (metadata, exception) -> {
                        if (exception != null)
                            // Forwarding kafka error to Mono
                            sink.error(exception);
                        else
                            // Forwarding send operation success to Mono
                            sink.success(metadata);
                    });
            // long elapsed = System.nanoTime() - start;
            // System.out.println(elapsed);

            // Canceling kafka operation if Mono was canceled, due to some downstream event
            sink.onCancel(() -> sendFuture.cancel(false));
        });
    }

    public static <K, V> Flux<ConsumerRecords<K, V>>
    consume(Consumer<K, V> consumer, Duration pollDuration, FluxSink.OverflowStrategy strategy) {
        return consume(() -> consumer, pollDuration, strategy);
    }

    public static <K, V> Flux<ConsumerRecords<K, V>>
    consume(Supplier<Consumer<K, V>> consumerFactory,
            Duration pollDuration,
            FluxSink.OverflowStrategy strategy) {
        return Flux.create(sink -> {
            var consumer = consumerFactory.get();

            var semaphore = new TerminableLongSemaphore();
            // permit acquisition
            sink.onRequest(semaphore::release);
            // stop acquisition & consumer when sink is disposed
            sink.onDispose(() -> {
                semaphore.terminate();
                consumer.wakeup();
            });

            var thread = new Thread(() -> {
                try (consumer) {
                    while (semaphore.acquire(1) != -1)
                        sink.next(consumer.poll(pollDuration));

                    logger.debug("gracefully stopping consume");
                    sink.complete();
                } catch (WakeupException wa) {
                    logger.debug("gracefully stopping consume");
                    sink.complete();
                    // OK here
                } catch (Throwable ex) {
                    logger.error("error", ex);
                    sink.error(ex);
                } finally {
                    logger.info("gracefully stopping the consumer");
                }
            });

            // start thread
            thread.start();

        }, strategy);
    }

    public static void startKafkaStateFolderMonitoring(String appName) {
        MonitoringUtil.monitorAppFolder(Path.of("/kafka-state"), appName);
    }

    public static void waitKafkaAppRunning(KafkaStreams streams) {
        try {
            int subsequentErrors = 0;
            while (streams.state() != KafkaStreams.State.RUNNING) {
                if (streams.state() == KafkaStreams.State.ERROR)
                    subsequentErrors++;
                else
                    subsequentErrors = 0;

                if (subsequentErrors > 20) {
                    logger.info("Kafka app can't initialize.");
                    System.exit(1);
                }

                Thread.sleep(500);
                logger.info("Kafka app state {}", streams.state());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        logger.info("Kafka app running");
    }
}
