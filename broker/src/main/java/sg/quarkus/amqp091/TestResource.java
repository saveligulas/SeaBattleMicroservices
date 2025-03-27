package sg.quarkus.amqp091;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/test")
@Singleton
public class TestResource {
    @Inject
    AmqpBroker amqpBroker;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String test() {
        return "AmqpBroker should be initialized!";
    }
}
