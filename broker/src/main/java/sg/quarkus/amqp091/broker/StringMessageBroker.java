package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.Channel;
import sg.quarkus.amqp091.StringMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple broker implementation that handles string messages.
 * This works alongside the regular BrokerManager to provide string-specific
 * message handling.
 */
public class StringMessageBroker {
    private static final Logger logger = LoggerFactory.getLogger(StringMessageBroker.class);
    
    private static StringMessageBroker instance;
    
    // Map of queue name to list of consumer channels and tags
    private final Map<String, Map<Channel, String>> queueConsumers = new ConcurrentHashMap<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private StringMessageBroker() {
    }
    
    /**
     * Gets the singleton instance.
     *
     * @return The StringMessageBroker instance
     */
    public static synchronized StringMessageBroker getInstance() {
        if (instance == null) {
            instance = new StringMessageBroker();
        }
        return instance;
    }
    
    /**
     * Registers a new consumer.
     *
     * @param queueName The queue name
     * @param consumerTag The consumer tag
     * @param channel The channel
     */
    public void registerConsumer(String queueName, String consumerTag, Channel channel) {
        Map<Channel, String> consumers = queueConsumers.computeIfAbsent(queueName, k -> new ConcurrentHashMap<>());
        consumers.put(channel, consumerTag);
        logger.info("Registered string consumer {} for queue {}", consumerTag, queueName);
    }
    
    /**
     * Unregisters a consumer.
     *
     * @param queueName The queue name
     * @param consumerTag The consumer tag
     * @return True if the consumer was found and removed, false otherwise
     */
    public boolean unregisterConsumer(String queueName, String consumerTag) {
        Map<Channel, String> consumers = queueConsumers.get(queueName);
        if (consumers == null) {
            return false;
        }
        
        boolean removed = false;
        for (Map.Entry<Channel, String> entry : consumers.entrySet()) {
            if (entry.getValue().equals(consumerTag)) {
                consumers.remove(entry.getKey());
                removed = true;
                break;
            }
        }
        
        if (consumers.isEmpty()) {
            queueConsumers.remove(queueName);
        }
        
        if (removed) {
            logger.info("Unregistered string consumer {} for queue {}", consumerTag, queueName);
        }
        
        return removed;
    }
    
    /**
     * Publishes a string message to a queue.
     *
     * @param queueName The queue name
     * @param messageContent The message content as a string
     * @param exchange The exchange name
     * @param routingKey The routing key
     * @param properties The message properties
     * @return True if the message was delivered to at least one consumer, false otherwise
     */
    public boolean publishToQueue(String queueName, String messageContent, String exchange, 
                                String routingKey, Map<String, Object> properties) {
        Map<Channel, String> consumers = queueConsumers.get(queueName);
        if (consumers == null || consumers.isEmpty()) {
            logger.debug("No string consumers for queue {}", queueName);
            return false;
        }
        
        boolean delivered = false;
        StringMessage message = new StringMessage(messageContent, exchange, routingKey, properties);
        
        for (Map.Entry<Channel, String> entry : consumers.entrySet()) {
            Channel channel = entry.getKey();
            String consumerTag = entry.getValue();
            
            boolean success = channel.deliverStringMessage(consumerTag, message);
            if (success) {
                delivered = true;
                logger.debug("Delivered string message to consumer {} on queue {}", consumerTag, queueName);
            }
        }
        
        return delivered;
    }
    
    /**
     * Publishes a binary message to a queue, converting it to a string if possible.
     *
     * @param queueName The queue name
     * @param messageBody The message body as bytes
     * @param exchange The exchange name
     * @param routingKey The routing key
     * @param properties The message properties
     * @return True if the message was delivered to at least one consumer, false otherwise
     */
    public boolean publishBinaryToQueue(String queueName, byte[] messageBody, String exchange,
                                      String routingKey, Map<String, Object> properties) {
        // Try to convert to string using UTF-8
        try {
            String content = new String(messageBody, StandardCharsets.UTF_8);
            return publishToQueue(queueName, content, exchange, routingKey, properties);
        } catch (Exception e) {
            logger.warn("Could not convert binary message to string for queue {}", queueName, e);
            return false;
        }
    }
    
    /**
     * Checks if a queue has any consumers.
     *
     * @param queueName The queue name
     * @return True if the queue has consumers, false otherwise
     */
    public boolean hasConsumers(String queueName) {
        Map<Channel, String> consumers = queueConsumers.get(queueName);
        return consumers != null && !consumers.isEmpty();
    }
    
    /**
     * Gets the number of consumers for a queue.
     *
     * @param queueName The queue name
     * @return The number of consumers
     */
    public int getConsumerCount(String queueName) {
        Map<Channel, String> consumers = queueConsumers.get(queueName);
        return consumers != null ? consumers.size() : 0;
    }
}
