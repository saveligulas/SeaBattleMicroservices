package sg.quarkus.amqp091.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Queue {
    private static final Logger logger = LoggerFactory.getLogger(Queue.class);
    
    private final String name;
    private final boolean durable;
    private final boolean exclusive;
    private final boolean autoDelete;
    private final Map<String, Object> arguments;
    private final BlockingQueue<Message> messages = new LinkedBlockingQueue<>();
    private final Set<AmqpConsumer> consumers = ConcurrentHashMap.newKeySet();
    private final Set<String> consumerTags = ConcurrentHashMap.newKeySet();
    private final AtomicLong deliveryTagCounter = new AtomicLong(0);
    
    public Queue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
        this.name = name;
        this.durable = durable;
        this.exclusive = exclusive;
        this.autoDelete = autoDelete;
        this.arguments = arguments != null ? arguments : Collections.emptyMap();
    }
    
    public void enqueue(Message message) {
        boolean delivered = false;
        
        // Try to deliver directly to a consumer first (basic.consume)
        if (!consumers.isEmpty()) {
            for (AmqpConsumer consumer : consumers) {
                if (consumer.isActive() && !consumer.isBusy()) {
                    delivered = consumer.deliver(message, nextDeliveryTag());
                    if (delivered) {
                        logger.debug("Delivered message directly to consumer {}", consumer.getConsumerTag());
                        break;
                    }
                }
            }
        }
        
        // If not delivered to a consumer, add to the queue
        if (!delivered) {
            messages.add(message);
            logger.debug("Enqueued message to queue {}, queue size now {}", name, messages.size());
            
            // After adding to queue, check if any consumers are waiting
            dispatchToConsumers();
        }
    }
    
    public Message dequeue() {
        try {
            Message message = messages.poll();
            if (message != null) {
                logger.debug("Dequeued message from queue {}, queue size now {}", name, messages.size());
            }
            return message;
        } catch (Exception e) {
            logger.error("Error dequeuing message from queue {}", name, e);
            return null;
        }
    }
    
    public void dispatchToConsumers() {
        if (messages.isEmpty() || consumers.isEmpty()) {
            return;
        }
        
        for (AmqpConsumer consumer : consumers) {
            if (messages.isEmpty()) {
                break;
            }
            
            if (consumer.isActive() && !consumer.isBusy()) {
                Message message = dequeue();
                if (message != null) {
                    boolean delivered = consumer.deliver(message, nextDeliveryTag());
                    if (!delivered) {
                        // If delivery fails, put the message back at the front of the queue
                        messages.add(message);
                        logger.warn("Failed to deliver message to consumer {}, returned to queue", 
                                consumer.getConsumerTag());
                    } else {
                        logger.debug("Dispatched message to consumer {}", consumer.getConsumerTag());
                    }
                }
            }
        }
    }
    
    public AmqpConsumer addConsumer(String consumerTag, boolean noLocal, boolean noAck, boolean exclusive, String connectionId) {
        if (exclusive && !consumers.isEmpty()) {
            logger.warn("Cannot add exclusive consumer to queue {} as it already has consumers", name);
            return null;
        }
        
        if (consumerTags.contains(consumerTag)) {
            logger.warn("Consumer tag {} is already in use for queue {}", consumerTag, name);
            return null;
        }
        
        AmqpConsumer consumer = new AmqpConsumer(consumerTag, this, noLocal, noAck, exclusive, connectionId);
        consumers.add(consumer);
        consumerTags.add(consumerTag);
        logger.info("Added consumer {} to queue {}", consumerTag, name);
        
        // Trigger dispatch in case there are already messages waiting
        dispatchToConsumers();
        
        return consumer;
    }
    
    public AmqpConsumer addConsumer(String consumerTag, boolean noLocal, boolean noAck, boolean exclusive) {
        return addConsumer(consumerTag, noLocal, noAck, exclusive, null);
    }
    
    public boolean removeConsumer(String consumerTag) {
        boolean removed = consumers.removeIf(consumer -> consumer.getConsumerTag().equals(consumerTag));
        if (removed) {
            consumerTags.remove(consumerTag);
            logger.info("Removed consumer {} from queue {}", consumerTag, name);
            
            // If this is an auto-delete queue and there are no consumers, schedule it for deletion
            if (autoDelete && consumers.isEmpty()) {
                logger.info("Auto-delete queue {} has no consumers, scheduled for deletion", name);
                // In a real implementation, this would trigger a cleanup process
            }
        }
        return removed;
    }
    
    public boolean hasConsumers() {
        return !consumers.isEmpty();
    }
    
    public int getConsumerCount() {
        return consumers.size();
    }
    
    public int getMessageCount() {
        return messages.size();
    }
    
    public void purge() {
        int count = messages.size();
        messages.clear();
        logger.info("Purged {} messages from queue {}", count, name);
    }
    
    public long nextDeliveryTag() {
        return deliveryTagCounter.incrementAndGet();
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isDurable() {
        return durable;
    }
    
    public boolean isExclusive() {
        return exclusive;
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
        if (!(o instanceof Queue queue)) return false;
        return name.equals(queue.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
}