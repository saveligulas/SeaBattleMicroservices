package sg.quarkus.amqp091.broker;

import java.util.UUID;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@Startup
public class BrokerManager {
    private static BrokerManager instance;
    
    private static final Logger logger = LoggerFactory.getLogger(BrokerManager.class);
    
    private final Map<String, Exchange> exchanges = new ConcurrentHashMap<>();
    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    
    // Default exchange names
    public static final String DEFAULT_EXCHANGE = "";
    public static final String AMQP_DIRECT_EXCHANGE = "amq.direct";
    public static final String AMQP_FANOUT_EXCHANGE = "amq.fanout";
    public static final String AMQP_TOPIC_EXCHANGE = "amq.topic";
    public static final String AMQP_HEADERS_EXCHANGE = "amq.headers";
    
    @PostConstruct
    public void init() {
        logger.info("Initializing AMQP broker");
        
        // Store singleton instance for static access
        instance = this;
        
        // Create default exchanges
        createDefaultExchanges();
    }
    
    /**
     * Gets the singleton instance of BrokerManager.
     * This is used for components that can't use dependency injection.
     * 
     * @return The BrokerManager instance
     */
    public static BrokerManager getInstance() {
        return instance;
    }
    
    private void createDefaultExchanges() {
        // Default exchange (direct type, unnamed)
        DirectExchange defaultExchange = new DirectExchange(DEFAULT_EXCHANGE, true, false, null);
        exchanges.put(DEFAULT_EXCHANGE, defaultExchange);
        
        // Standard exchanges
        exchanges.put(AMQP_DIRECT_EXCHANGE, new DirectExchange(AMQP_DIRECT_EXCHANGE, true, false, null));
        exchanges.put(AMQP_FANOUT_EXCHANGE, new FanoutExchange(AMQP_FANOUT_EXCHANGE, true, false, null));
        exchanges.put(AMQP_TOPIC_EXCHANGE, new TopicExchange(AMQP_TOPIC_EXCHANGE, true, false, null));
        exchanges.put(AMQP_HEADERS_EXCHANGE, new HeadersExchange(AMQP_HEADERS_EXCHANGE, true, false, null));
        
        logger.info("Created default exchanges");
    }
    
    public Exchange declareExchange(String name, String type, boolean durable, boolean autoDelete, Map<String, Object> arguments) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Default exchange cannot be redeclared");
        }
        
        // Check if exchange exists
        Exchange existingExchange = exchanges.get(name);
        if (existingExchange != null) {
            // Check if the declaration is compatible
            if (existingExchange.getClass().getSimpleName().toLowerCase().startsWith(type.toLowerCase()) &&
                existingExchange.isDurable() == durable &&
                existingExchange.isAutoDelete() == autoDelete) {
                
                logger.debug("Exchange {} already exists with compatible parameters", name);
                return existingExchange;
            } else {
                logger.error("Exchange {} already exists with incompatible parameters", name);
                throw new IllegalArgumentException("Exchange already exists with incompatible parameters");
            }
        }
        
        // Create new exchange
        Exchange exchange = Exchange.createExchange(name, type, durable, autoDelete, arguments);
        exchanges.put(name, exchange);
        logger.info("Declared exchange {}, type: {}, durable: {}, autoDelete: {}", 
                name, type, durable, autoDelete);
        
        return exchange;
    }
    
    public boolean deleteExchange(String name, boolean ifUnused) {
        if (name.isEmpty() || name.startsWith("amq.")) {
            logger.error("Cannot delete built-in exchange: {}", name);
            throw new IllegalArgumentException("Cannot delete built-in exchange");
        }
        
        Exchange exchange = exchanges.get(name);
        if (exchange == null) {
            logger.warn("Exchange {} does not exist", name);
            return false;
        }
        
        // If ifUnused is true, check if there are any bindings
        if (ifUnused) {
            // This is a simplified check, a real implementation would need to check all bindings
            for (Queue queue : queues.values()) {
                // In a real implementation, we would check if the queue is bound to this exchange
                // Here we just log a warning for simplicity
                logger.warn("Cannot check if exchange {} is unused, assuming it's not", name);
                return false;
            }
        }
        
        exchanges.remove(name);
        logger.info("Deleted exchange {}", name);
        return true;
    }
    
    public Queue declareQueue(String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments) {
        Queue queue;
        
        // Check if queue exists
        if (!name.isEmpty()) {
            queue = queues.get(name);
            if (queue != null) {
                // Check if the declaration is compatible
                if (queue.isDurable() == durable &&
                    queue.isExclusive() == exclusive &&
                    queue.isAutoDelete() == autoDelete) {
                    
                    logger.debug("Queue {} already exists with compatible parameters", name);
                    return queue;
                } else {
                    logger.error("Queue {} already exists with incompatible parameters", name);
                    throw new IllegalArgumentException("Queue already exists with incompatible parameters");
                }
            }
        }
        
        // Generate a name for server-named queues
        String actualName = name.isEmpty() ? generateQueueName() : name;
        
        // Create new queue
        queue = new Queue(actualName, durable, exclusive, autoDelete, arguments);
        queues.put(actualName, queue);
        logger.info("Declared queue {}, durable: {}, exclusive: {}, autoDelete: {}", 
                actualName, durable, exclusive, autoDelete);
        
        // Auto-bind server-named queues to the default exchange with their name as the routing key
        if (name.isEmpty()) {
            Exchange defaultExchange = exchanges.get(DEFAULT_EXCHANGE);
            defaultExchange.addBinding(actualName, queue, null);
            logger.info("Auto-bound queue {} to default exchange with routing key {}", actualName, actualName);
        }
        
        return queue;
    }
    
    public int deleteQueue(String name, boolean ifUnused, boolean ifEmpty) {
        Queue queue = queues.get(name);
        if (queue == null) {
            logger.warn("Queue {} does not exist", name);
            return 0;
        }
        
        // Check conditions
        if (ifUnused && queue.hasConsumers()) {
            logger.warn("Cannot delete queue {} as it has active consumers", name);
            return 0;
        }
        
        if (ifEmpty && queue.getMessageCount() > 0) {
            logger.warn("Cannot delete queue {} as it is not empty", name);
            return 0;
        }
        
        // Remove all bindings to the queue
        for (Exchange exchange : exchanges.values()) {
            exchange.removeAllBindingsForQueue(queue);
        }
        
        // Get message count before deletion
        int messageCount = queue.getMessageCount();
        
        // Remove queue
        queues.remove(name);
        logger.info("Deleted queue {} with {} messages", name, messageCount);
        
        return messageCount;
    }
    
    public int purgeQueue(String name) {
        Queue queue = queues.get(name);
        if (queue == null) {
            logger.warn("Queue {} does not exist", name);
            return 0;
        }
        
        int messageCount = queue.getMessageCount();
        queue.purge();
        logger.info("Purged queue {} of {} messages", name, messageCount);
        
        return messageCount;
    }
    
    public Binding bindQueue(String queueName, String exchangeName, String routingKey, Map<String, Object> arguments) {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            logger.error("Queue {} does not exist", queueName);
            throw new IllegalArgumentException("Queue does not exist");
        }
        
        Exchange exchange = exchanges.get(exchangeName);
        if (exchange == null) {
            logger.error("Exchange {} does not exist", exchangeName);
            throw new IllegalArgumentException("Exchange does not exist");
        }
        
        // Use empty string as default routing key
        String actualRoutingKey = routingKey == null || routingKey.isEmpty() ? "" : routingKey;
        
        exchange.addBinding(actualRoutingKey, queue, arguments);
        logger.info("Bound queue {} to exchange {} with routing key '{}'", queueName, exchangeName, actualRoutingKey);
        
        return new Binding(actualRoutingKey, queue, exchange, arguments);
    }
    
    public boolean unbindQueue(String queueName, String exchangeName, String routingKey) {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            logger.warn("Queue {} does not exist", queueName);
            return false;
        }
        
        Exchange exchange = exchanges.get(exchangeName);
        if (exchange == null) {
            logger.warn("Exchange {} does not exist", exchangeName);
            return false;
        }
        
        // Use empty string as default routing key
        String actualRoutingKey = routingKey == null || routingKey.isEmpty() ? "" : routingKey;
        
        exchange.removeBinding(actualRoutingKey, queue);
        logger.info("Unbound queue {} from exchange {} with routing key '{}'", queueName, exchangeName, actualRoutingKey);
        
        return true;
    }
    
    public AmqpConsumer addConsumer(String queueName, String consumerTag, boolean noLocal, boolean noAck, boolean exclusive, String connectionId) {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            logger.error("Queue {} does not exist", queueName);
            throw new IllegalArgumentException("Queue does not exist");
        }
        
        // Generate a consumer tag if not provided
        String actualConsumerTag = consumerTag == null || consumerTag.isEmpty() ? 
                generateConsumerTag() : consumerTag;
        
        AmqpConsumer consumer = queue.addConsumer(actualConsumerTag, noLocal, noAck, exclusive, connectionId);
        if (consumer == null) {
            logger.error("Failed to add consumer to queue {}", queueName);
            throw new IllegalStateException("Failed to add consumer");
        }
        
        logger.info("Added consumer {} to queue {}", actualConsumerTag, queueName);
        return consumer;
    }
    
    public AmqpConsumer addConsumer(String queueName, String consumerTag, boolean noLocal, boolean noAck, boolean exclusive) {
        return addConsumer(queueName, consumerTag, noLocal, noAck, exclusive, null);
    }
    
    public boolean cancelConsumer(String queueName, String consumerTag) {
        Queue queue = queues.get(queueName);
        if (queue == null) {
            logger.warn("Queue {} does not exist", queueName);
            return false;
        }
        
        boolean cancelled = queue.removeConsumer(consumerTag);
        if (cancelled) {
            logger.info("Cancelled consumer {} on queue {}", consumerTag, queueName);
        } else {
            logger.warn("Consumer {} not found on queue {}", consumerTag, queueName);
        }
        
        return cancelled;
    }
    
    public void publishMessage(String exchangeName, String routingKey, Map<String, Object> headers, 
                             Map<String, Object> properties, byte[] body, String connectionId) {
        
        Exchange exchange = exchanges.get(exchangeName);
        if (exchange == null) {
            if (true) {
                exchange = exchanges.get(DEFAULT_EXCHANGE); //TODO: remove this in future when exchanges work
            } else {
                logger.error("Exchange {} does not exist", exchangeName);
                throw new IllegalArgumentException("Exchange does not exist");
            }

        }
        
        Message message = Message.builder()
                .exchange(exchangeName)
                .routingKey(routingKey)
                .headers(headers)
                .properties(properties)
                .body(body)
                .connectionId(connectionId)
                .build();
        
        exchange.route(message, routingKey);
    }
    
    public Exchange getExchange(String name) {
        return exchanges.get(name);
    }
    
    public Queue getQueue(String name) {
        return queues.get(name);
    }
    
    public Map<String, Exchange> getExchanges() {
        return Collections.unmodifiableMap(exchanges);
    }
    
    public Map<String, Queue> getQueues() {
        return Collections.unmodifiableMap(queues);
    }
    
    private String generateQueueName() {
        return "amq.gen-" + UUID.randomUUID().toString();
    }
    
    private String generateConsumerTag() {
        return "amq.ctag-" + UUID.randomUUID().toString();
    }
}