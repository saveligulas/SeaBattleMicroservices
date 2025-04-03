package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AmqpConsumer {
    private static final Logger logger = LoggerFactory.getLogger(AmqpConsumer.class);
    
    private final String consumerTag;
    private final Queue queue;
    private final boolean noLocal;
    private final boolean noAck;
    private final boolean exclusive;
    private final String connectionId;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final Set<Long> unacknowledgedMessages = new HashSet<>();
    
    private Consumer<DeliveryContext> deliveryCallback;
    
    public AmqpConsumer(String consumerTag, Queue queue, boolean noLocal, boolean noAck, boolean exclusive, String connectionId) {
        this.consumerTag = consumerTag;
        this.queue = queue;
        this.noLocal = noLocal;
        this.noAck = noAck;
        this.exclusive = exclusive;
        this.connectionId = connectionId;
    }
    
    public boolean deliver(Message message, long deliveryTag) {
        if (!active.get()) {
            return false;
        }
        
        try {
            busy.set(true);
            
            // Check if this is a message that should be filtered due to noLocal flag
            if (noLocal && message.isFromSameConnection(connectionId)) {
                logger.debug("Skipping message delivery to consumer {} due to noLocal flag", consumerTag);
                busy.set(false);
                return true; // Message handled (skipped), not requeued
            }
            
            DeliveryContext deliveryContext = new DeliveryContext(
                    message, 
                    deliveryTag, 
                    consumerTag, 
                    queue.getName(), 
                    message.getExchange(), 
                    message.getRoutingKey(), 
                    false); // redelivered flag set to false for first delivery
            
            if (deliveryCallback != null) {
                deliveryCallback.accept(deliveryContext);
                
                if (!noAck) {
                    // If acknowledgment is required, track this message
                    unacknowledgedMessages.add(deliveryTag);
                }
                
                logger.debug("Delivered message (tag={}) to consumer {}", deliveryTag, consumerTag);
                return true;
            } else {
                logger.warn("No delivery callback registered for consumer {}", consumerTag);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error delivering message to consumer {}", consumerTag, e);
            return false;
        } finally {
            busy.set(false);
        }
    }
    
    public void acknowledge(long deliveryTag, boolean multiple) {
        if (multiple) {
            unacknowledgedMessages.removeIf(tag -> tag <= deliveryTag);
            logger.debug("Consumer {} acknowledged multiple messages up to tag {}", consumerTag, deliveryTag);
        } else {
            unacknowledgedMessages.remove(deliveryTag);
            logger.debug("Consumer {} acknowledged message with tag {}", consumerTag, deliveryTag);
        }
    }
    
    public void reject(long deliveryTag, boolean requeue) {
        unacknowledgedMessages.remove(deliveryTag);
        logger.debug("Consumer {} rejected message with tag {}, requeue={}", consumerTag, deliveryTag, requeue);
        
        // In a real implementation, if requeue is true, the message would be returned to the queue
    }
    
    public void cancel() {
        active.set(false);
        queue.removeConsumer(consumerTag);
        logger.info("Consumer {} cancelled", consumerTag);
    }
    
    public void setDeliveryCallback(Consumer<DeliveryContext> deliveryCallback) {
        this.deliveryCallback = deliveryCallback;
    }
    
    public String getConsumerTag() {
        return consumerTag;
    }
    
    public Queue getQueue() {
        return queue;
    }
    
    public boolean isNoLocal() {
        return noLocal;
    }
    
    public boolean isNoAck() {
        return noAck;
    }
    
    public boolean isExclusive() {
        return exclusive;
    }
    
    public boolean isActive() {
        return active.get();
    }
    
    public boolean isBusy() {
        return busy.get();
    }
    
    public int getUnacknowledgedMessageCount() {
        return unacknowledgedMessages.size();
    }
    
    public static class DeliveryContext {
        private final Message message;
        private final long deliveryTag;
        private final String consumerTag;
        private final String queue;
        private final String exchange;
        private final String routingKey;
        private final boolean redelivered;
        
        public DeliveryContext(Message message, long deliveryTag, String consumerTag, 
                              String queue, String exchange, String routingKey, boolean redelivered) {
            this.message = message;
            this.deliveryTag = deliveryTag;
            this.consumerTag = consumerTag;
            this.queue = queue;
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.redelivered = redelivered;
        }
        
        public Message getMessage() {
            return message;
        }
        
        public long getDeliveryTag() {
            return deliveryTag;
        }
        
        public String getConsumerTag() {
            return consumerTag;
        }
        
        public String getQueue() {
            return queue;
        }
        
        public String getExchange() {
            return exchange;
        }
        
        public String getRoutingKey() {
            return routingKey;
        }
        
        public boolean isRedelivered() {
            return redelivered;
        }
    }
}