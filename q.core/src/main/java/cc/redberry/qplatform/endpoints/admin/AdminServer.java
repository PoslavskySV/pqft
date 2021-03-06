package cc.redberry.qplatform.endpoints.admin;

import cc.redberry.qplatform.cluster.KafkaInit;
import cc.redberry.qplatform.cluster.KafkaTopics;
import cc.redberry.qplatform.endpoints.ServerUtil;

/**
 *
 */
public class AdminServer {
    private AdminServer() {}

    public static void main(String[] args) throws Exception {
        KafkaTopics.initAll();

        try (var qgraf = new QgrafController()) {
            ServerUtil.runServer("admin",
                    true,
                    true,
                    new PingController(),
                    new TheoriesController(),
                    qgraf);
        }
    }

    /** Preferences endpoint */
    static final String ENDPOINT_ADMIN = System.getenv().getOrDefault("ENDPOINT_ADMIN", "http://app-endpoints-admin:8080");
}
