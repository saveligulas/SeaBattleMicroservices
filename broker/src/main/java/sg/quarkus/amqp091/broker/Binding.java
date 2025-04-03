package sg.quarkus.amqp091.broker;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class Binding {
    private final String routingKey;
    private final Queue queue;
    private final Exchange exchange;
    private final Map<String, Object> arguments;
    
    public Binding(String routingKey, Queue queue, Exchange exchange, Map<String, Object> arguments) {
        this.routingKey = routingKey;
        this.queue = queue;
        this.exchange = exchange;
        this.arguments = arguments != null ? arguments : Collections.emptyMap();
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public Queue getQueue() {
        return queue;
    }
    
    public Exchange getExchange() {
        return exchange;
    }
    
    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Binding binding)) return false;
        return Objects.equals(routingKey, binding.routingKey) &&
                Objects.equals(queue, binding.queue) &&
                Objects.equals(exchange, binding.exchange);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(routingKey, queue, exchange);
    }
}