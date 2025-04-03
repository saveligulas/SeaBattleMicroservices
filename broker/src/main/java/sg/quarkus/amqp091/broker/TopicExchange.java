package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TopicExchange extends Exchange {
    private static final Logger logger = LoggerFactory.getLogger(TopicExchange.class);
    
    public TopicExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        super(name, durable, autoDelete, arguments);
    }
    
    @Override
    public void route(Message message, String routingKey) {
        int routedCount = 0;
        
        // Check all binding patterns against this routing key
        for (Map.Entry<String, Set<Binding>> entry : bindings.entrySet()) {
            String bindingPattern = entry.getKey();
            
            if (matchesTopicPattern(bindingPattern, routingKey)) {
                for (Binding binding : entry.getValue()) {
                    binding.getQueue().enqueue(message);
                    routedCount++;
                    logger.debug("Routed message to queue {} via topic exchange {} with pattern '{}' matching key '{}'", 
                            binding.getQueue().getName(), name, bindingPattern, routingKey);
                }
            }
        }
        
        if (routedCount == 0) {
            logger.debug("No matching bindings found for routing key '{}' in topic exchange {}", routingKey, name);
        }
    }
    
    private boolean matchesTopicPattern(String pattern, String routingKey) {
        // Convert AMQP pattern to regex pattern
        // * (star) matches exactly one word
        // # (hash) matches zero or more words
        String regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", "[^\\.]+")
                .replace("#", ".*");
        
        return Pattern.matches("^" + regexPattern + "$", routingKey);
    }
}