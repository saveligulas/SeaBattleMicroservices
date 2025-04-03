package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Exchange {
    private static final Logger logger = LoggerFactory.getLogger(Exchange.class);
    
    protected final String name;
    protected final boolean durable;
    protected final boolean autoDelete;
    protected final Map<String, Object> arguments;
    protected final Map<String, Set<Binding>> bindings = new ConcurrentHashMap<>();
    
    public enum Type {
        DIRECT("direct"),
        FANOUT("fanout"),
        TOPIC("topic"),
        HEADERS("headers");
        
        private final String typeName;
        
        Type(String typeName) {
            this.typeName = typeName;
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        public static Type fromString(String typeName) {
            for (Type type : values()) {
                if (type.getTypeName().equals(typeName)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown exchange type: " + typeName);
        }
    }
    
    protected Exchange(String name, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        this.name = name;
        this.durable = durable;
        this.autoDelete = autoDelete;
        this.arguments = arguments != null ? arguments : Collections.emptyMap();
    }
    
    public static Exchange createExchange(String name, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        Type exchangeType = Type.fromString(type);
        return switch (exchangeType) {
            case DIRECT -> new DirectExchange(name, durable, autoDelete, arguments);
            case FANOUT -> new FanoutExchange(name, durable, autoDelete, arguments);
            case TOPIC -> new TopicExchange(name, durable, autoDelete, arguments);
            case HEADERS -> new HeadersExchange(name, durable, autoDelete, arguments);
        };
    }
    
    public void addBinding(String routingKey, Queue queue, Map<String, Object> arguments) {
        Binding binding = new Binding(routingKey, queue, this, arguments);
        bindings.computeIfAbsent(routingKey, k -> ConcurrentHashMap.newKeySet()).add(binding);
        logger.debug("Added binding from exchange {} to queue {} with routing key '{}'", name, queue.getName(), routingKey);
    }
    
    public void removeBinding(String routingKey, Queue queue) {
        Set<Binding> routingBindings = bindings.get(routingKey);
        if (routingBindings != null) {
            routingBindings.removeIf(binding -> binding.getQueue().equals(queue));
            if (routingBindings.isEmpty()) {
                bindings.remove(routingKey);
            }
            logger.debug("Removed binding from exchange {} to queue {} with routing key '{}'", name, queue.getName(), routingKey);
        }
    }
    
    public void removeAllBindingsForQueue(Queue queue) {
        bindings.forEach((routingKey, bindingSet) -> 
            bindingSet.removeIf(binding -> binding.getQueue().equals(queue)));
        
        // Clean up empty binding sets
        bindings.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        logger.debug("Removed all bindings for queue {} from exchange {}", queue.getName(), name);
    }
    
    public abstract void route(Message message, String routingKey);
    
    public String getName() {
        return name;
    }
    
    public boolean isDurable() {
        return durable;
    }
    
    public boolean isAutoDelete() {
        return autoDelete;
    }
    
    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exchange exchange)) return false;
        return name.equals(exchange.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}