package cc.redberry.qplatform.util;

import cc.redberry.qplatform.cluster.Config;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

public class MonitoringUtil {
    private static final Logger log = LoggerFactory.getLogger(MonitoringUtil.class);

    private MonitoringUtil() {}

    private static boolean initialized = false;

    public static void ensureMonitoringInitialized() {
        if (!initialized)
            synchronized (MonitoringUtil.class) {
                if (!initialized) {
                    // common tags
                    Metrics.globalRegistry.config().commonTags("env", Config.getEnv());

                    // add Jvm metrics
                    // new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
                    // new JvmGcMetrics().bindTo(Metrics.globalRegistry);
                    // new ProcessorMetrics().bindTo(Metrics.globalRegistry);
                    // new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
                    // new KafkaConsumerMetrics().bindTo(Metrics.globalRegistry);
                }
            }
    }

    // private static Timer timer = null;
    //
    // private static void ensureTimer() {
    //     if (timer == null)
    //         synchronized (MonitoringUtil.class) {
    //             if (timer == null) {
    //                 timer = new Timer("monitoring-timer", true);
    //             }
    //         }
    // }

    public static void monitorAppFolder(Path path, String appName) {
        try {
            FileStore fileStore = Files.getFileStore(path);

            Gauge
                    .builder("app.folder.total", () -> {
                        try {
                            return fileStore.getTotalSpace();
                        } catch (IOException e) {
                            log.error("While retrieving total size.", e);
                            return 0;
                        }
                    })
                    .description("Total space available for the app's data folder.")
                    .tags("app", appName, "path", path.toString())
                    .register(Metrics.globalRegistry);

            Gauge
                    .builder("app.folder.free", () -> {
                        try {
                            return fileStore.getUsableSpace();
                        } catch (IOException e) {
                            log.error("While retrieving total size.", e);
                            return 0;
                        }
                    })
                    .description("Free space available in the app's data folder.")
                    .tags("app", appName, "path", path.toString())
                    .register(Metrics.globalRegistry);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
