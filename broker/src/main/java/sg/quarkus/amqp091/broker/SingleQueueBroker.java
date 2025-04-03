package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.Channel;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class SingleQueueBroker {
    private static final Logger logger = LoggerFactory.getLogger(SingleQueueBroker.class);

    private static SingleQueueBroker instance;

    // Store messages with their metadata
    private static class StoredMessage {
        final byte[] body;
        final Map<String, Object> properties;
        final String exchange;
        final String routingKey;

        StoredMessage(byte[] body, Map<String, Object> properties, String exchange, String routingKey) {
            this.body = body;
            this.properties = properties != null ? properties : new HashMap<>();
            this.exchange = exchange;
            this.routingKey = routingKey;
        }
    }

    // Store both the consumer tag and the channel it belongs to
    private static class ConsumerInfo {
        final String tag;
        final Channel channel;
        final String queueName;

        ConsumerInfo(String tag, Channel channel, String queueName) {
            this.tag = tag;
            this.channel = channel;
            this.queueName = queueName;
        }
    }

    // Message queue
    private final ConcurrentLinkedQueue<StoredMessage> messageQueue = new ConcurrentLinkedQueue<>();

    // Consumer registry - Map from consumer tag to consumer info
    private final Map<String, ConsumerInfo> consumers = new ConcurrentHashMap<>();

    // Private constructor for singleton
    private SingleQueueBroker() {
        logger.info("SingleQueueBroker initialized");
    }

    /**
     * Gets the singleton instance
     */
    public static synchronized SingleQueueBroker getInstance() {
        if (instance == null) {
            instance = new SingleQueueBroker();
        }
        return instance;
    }

    /**
     * Registers a consumer to receive messages
     *
     * @param queueName The queue name (ignored in this simple implementation)
     * @param consumerTag The consumer tag or empty string for auto-generation
     * @param channel The channel that will deliver messages
     * @return The assigned consumer tag
     */
    public String registerConsumer(String queueName, String consumerTag, Channel channel) {
        // Generate a tag if none provided
        String actualTag = consumerTag;
        if (actualTag == null || actualTag.isEmpty()) {
            actualTag = "amq.ctag-" + Math.abs(UUID.randomUUID().getLeastSignificantBits());
        }

        // Register the consumer
        ConsumerInfo info = new ConsumerInfo(actualTag, channel, queueName);
        consumers.put(actualTag, info);
        logger.info("Registered consumer with tag: {}, queue: {}", actualTag, queueName);

        // Deliver any pending messages
        deliverPendingMessages();

        return actualTag;
    }

    /**
     * Unregisters a consumer
     *
     * @param consumerTag The consumer tag
     * @return True if the consumer was found and removed, false otherwise
     */
    public boolean unregisterConsumer(String consumerTag) {
        ConsumerInfo removed = consumers.remove(consumerTag);
        if (removed != null) {
            logger.info("Unregistered consumer with tag: {}", consumerTag);
            return true;
        }
        return false;
    }

    /**
     * Publishes a message
     */
    public void publishMessage(byte[] messageBody, Map<String, Object> properties,
                               String exchange, String routingKey) {
        if (messageBody == null) {
            logger.warn("Ignoring null message body");
            return;
        }

        // Create a stored message
        StoredMessage message = new StoredMessage(
                messageBody,
                properties,
                exchange,
                routingKey
        );

        // Try immediate delivery
        boolean delivered = deliverToConsumers(message);

        // If not delivered, queue for later
        if (!delivered) {
            messageQueue.add(message);
            String content = new String(messageBody, StandardCharsets.UTF_8);
            logger.info("Queued message: '{}' for later delivery, queue size: {}",
                    content, messageQueue.size());
        }
    }

    /**
     * Delivers pending messages to all consumers
     */
    private void deliverPendingMessages() {
        if (consumers.isEmpty() || messageQueue.isEmpty()) {
            return;
        }

        logger.debug("Attempting to deliver {} pending messages to {} consumers",
                messageQueue.size(), consumers.size());

        List<StoredMessage> messagesToDeliver = new ArrayList<>();
        StoredMessage message;
        while ((message = messageQueue.poll()) != null) {
            messagesToDeliver.add(message);
        }

        for (StoredMessage msg : messagesToDeliver) {
            boolean delivered = deliverToConsumers(msg);
            if (!delivered) {
                // Put back in queue if not delivered
                messageQueue.add(msg);
            }
        }

        logger.debug("After delivery attempt, {} messages remain in queue",
                messageQueue.size());
    }

    /**
     * Delivers a message to all registered consumers
     *
     * @param message The message to deliver
     * @return True if delivered to at least one consumer, false otherwise
     */
    private boolean deliverToConsumers(StoredMessage message) {
        if (consumers.isEmpty()) {
            return false;
        }

        boolean delivered = false;
        String content = null;

        // Loop through all consumers and try to deliver
        for (ConsumerInfo consumer : consumers.values()) {
            try {
                // Make sure the channel is still open
                if (consumer.channel != null) {
                    consumer.channel.deliverMessageWithActualTag(
                            consumer.tag,
                            message.body
                    );
                    delivered = true;

                    if (content == null) {
                        content = new String(message.body, StandardCharsets.UTF_8);
                    }

                    logger.debug("Delivered message: '{}' to consumer {}",
                            content, consumer.tag);
                }
            } catch (Exception e) {
                logger.error("Error delivering message to consumer {}: {}",
                        consumer.tag, e.getMessage());
            }
        }

        if (delivered) {
            if (content == null) {
                content = new String(message.body, StandardCharsets.UTF_8);
            }
            logger.info("Successfully delivered message: '{}' to consumers", content);
        }

        return delivered;
    }
}