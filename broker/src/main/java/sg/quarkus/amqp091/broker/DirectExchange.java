package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class DirectExchange extends Exchange {
    private static final Logger logger = LoggerFactory.getLogger(DirectExchange.class);
    
    public DirectExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        super(name, durable, autoDelete, arguments);
    }
    
    @Override
    public void route(Message message, String routingKey) {
        Set<Binding> routingBindings = bindings.get(routingKey);
        
        if (routingBindings != null && !routingBindings.isEmpty()) {
            for (Binding binding : routingBindings) {
                binding.getQueue().enqueue(message);
                logger.debug("Routed message to queue {} via direct exchange {} with routing key '{}'", 
                        binding.getQueue().getName(), name, routingKey);
            }
        } else {
            logger.debug("No bindings found for routing key '{}' in direct exchange {}", routingKey, name);
        }
    }
}