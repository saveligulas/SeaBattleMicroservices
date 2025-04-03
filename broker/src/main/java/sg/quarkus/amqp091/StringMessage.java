package sg.quarkus.amqp091;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple message class that holds a string content.
 * Simplified version for string-only messaging.
 */
public class StringMessage {
    private final String content;
    private final String exchange;
    private final String routingKey;
    private final Map<String, Object> properties;
    
    /**
     * Creates a new string message.
     *
     * @param content The message content as a string
     * @param exchange The exchange the message was published to
     * @param routingKey The routing key used
     * @param properties Message properties
     */
    public StringMessage(String content, String exchange, String routingKey, Map<String, Object> properties) {
        this.content = content;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.properties = new HashMap<>(properties);
    }
    
    /**
     * Gets the message content.
     *
     * @return The content as a string
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Gets the exchange the message was published to.
     *
     * @return The exchange name
     */
    public String getExchange() {
        return exchange;
    }
    
    /**
     * Gets the routing key used for the message.
     *
     * @return The routing key
     */
    public String getRoutingKey() {
        return routingKey;
    }
    
    /**
     * Gets the message properties.
     *
     * @return The properties map
     */
    public Map<String, Object> getProperties() {
        return new HashMap<>(properties);
    }
}
