package cc.redberry.qplatform.qgraf;

import io.micrometer.core.instrument.Counter;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

/**
 *
 */
public class QgrafMetrics {
    private QgrafMetrics() {}

    private static QgrafMetrics metrics;

    public static QgrafMetrics get() {
        if (metrics == null)
            synchronized (QgrafMetrics.class) {
                if (metrics == null)
                    metrics = new QgrafMetrics();
            }
        return metrics;
    }

    //////////////////////////////////////////////////// Qgraf App ////////////////////////////////////////////////////

    public final Counter calculationRequests = Counter
            .builder("qgraf.app.calculation.requests")
            .description("Number of diagrams generation requests")
            .register(globalRegistry);

    public final Counter errUnknown = Counter
            .builder("qgraf.app.err")
            .tag("type", "unknown")
            .description("Unhandled error during Qgraf run")
            .register(globalRegistry);
}
