package sg.quarkus.amqp091;

import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class VerticleDeployer {
    private static final Logger LOG = Logger.getLogger(VerticleDeployer.class);

    @Inject
    Vertx vertx;

    @PostConstruct
    public void init() {
        LOG.info("Starting AMQP Broker...");
        vertx.deployVerticle(new AMQPVerticle(), res -> {
            if (res.succeeded()) {
                LOG.info("✅ AMQP Broker started successfully!");
            } else {
                LOG.error("❌ Failed to start AMQP Broker: ", res.cause());
            }
        });
    }
}
