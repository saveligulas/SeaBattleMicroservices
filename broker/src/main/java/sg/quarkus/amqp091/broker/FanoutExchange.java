package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class FanoutExchange extends Exchange {
    private static final Logger logger = LoggerFactory.getLogger(FanoutExchange.class);
    
    public FanoutExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        super(name, durable, autoDelete, arguments);
    }
    
    @Override
    public void route(Message message, String routingKey) {
        // In a fanout exchange, we ignore the routing key and deliver to all queues
        int routedCount = 0;
        
        for (Set<Binding> bindingSet : bindings.values()) {
            for (Binding binding : bindingSet) {
                binding.getQueue().enqueue(message);
                routedCount++;
            }
        }
        
        if (routedCount > 0) {
            logger.debug("Routed message to {} queues via fanout exchange {}", routedCount, name);
        } else {
            logger.debug("No bindings found in fanout exchange {}", name);
        }
    }
}