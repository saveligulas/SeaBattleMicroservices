package sg.quarkus.amqp091;

import io.quarkus.runtime.Startup;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

@Singleton
@Startup
public class AmqpBroker {
    private static final Logger LOG = Logger.getLogger(AmqpBroker.class);

    @Inject
    Vertx vertx;

    @PostConstruct
    public void init() {
        LOG.info("Starting AMQP Broker...");
        vertx.deployVerticle(new AmqpVerticle(), res -> {
            if (res.succeeded()) {
                LOG.info("✅ AMQP Broker started successfully!");
            } else {
                LOG.error("❌ Failed to start AMQP Broker: ", res.cause());
            }
        });
    }
}
