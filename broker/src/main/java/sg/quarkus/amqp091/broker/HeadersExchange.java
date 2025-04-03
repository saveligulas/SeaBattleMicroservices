package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

public class HeadersExchange extends Exchange {
    private static final Logger logger = LoggerFactory.getLogger(HeadersExchange.class);
    private static final String X_MATCH = "x-match";
    
    public HeadersExchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        super(name, durable, autoDelete, arguments);
    }
    
    @Override
    public void route(Message message, String routingKey) {
        // In headers exchange, routing key is ignored; routing is based on message headers
        int routedCount = 0;
        Map<String, Object> messageHeaders = message.getHeaders();
        
        for (Set<Binding> bindingSet : bindings.values()) {
            for (Binding binding : bindingSet) {
                if (headersMatch(binding.getArguments(), messageHeaders)) {
                    binding.getQueue().enqueue(message);
                    routedCount++;
                    logger.debug("Routed message to queue {} via headers exchange {}", 
                            binding.getQueue().getName(), name);
                }
            }
        }
        
        if (routedCount == 0) {
            logger.debug("No matching bindings found for message headers in headers exchange {}", name);
        }
    }
    
    private boolean headersMatch(Map<String, Object> bindingArguments, Map<String, Object> messageHeaders) {
        if (bindingArguments == null || bindingArguments.isEmpty()) {
            return false;
        }
        
        // Default is "all" if not specified
        String matchType = "all";
        
        if (bindingArguments.containsKey(X_MATCH)) {
            matchType = bindingArguments.get(X_MATCH).toString().toLowerCase();
        }
        
        // Remove x-match from criteria
        Map<String, Object> criteria = Map.copyOf(bindingArguments);
        Map<String, Object> filteredCriteria = criteria.entrySet().stream()
                .filter(e -> !X_MATCH.equals(e.getKey()))
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        if (filteredCriteria.isEmpty()) {
            return true; // No criteria to match
        }
        
        if ("any".equals(matchType)) {
            // Match if any of the headers match
            return filteredCriteria.entrySet().stream()
                    .anyMatch(entry -> 
                        messageHeaders.containsKey(entry.getKey()) && 
                        entry.getValue().equals(messageHeaders.get(entry.getKey())));
        } else {
            // Match if all headers match (default)
            return filteredCriteria.entrySet().stream()
                    .allMatch(entry -> 
                        messageHeaders.containsKey(entry.getKey()) && 
                        entry.getValue().equals(messageHeaders.get(entry.getKey())));
        }
    }
}