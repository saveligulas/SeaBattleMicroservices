package sg.quarkus.amqp091;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.protocol.frame.AMQPPayloadHeaderType;
import sg.quarkus.amqp091.protocol.frame.FrameFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a consumer that receives messages from a queue.
 */
public class Consumer {
    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    
    private static final AtomicLong DELIVERY_TAG_GENERATOR = new AtomicLong(1);
    
    private final String consumerTag;
    private final Channel channel;
    private final short channelId;
    private final NetSocket socket;
    private final String queueName;
    private final boolean noAck;
    
    /**
     * Creates a new consumer.
     *
     * @param consumerTag The consumer tag
     * @param channel The channel this consumer belongs to
     * @param channelId The channel ID
     * @param socket The socket to write frames to
     * @param queueName The queue name to consume from
     * @param noAck Whether acknowledgments are required
     */
    public Consumer(String consumerTag, Channel channel, short channelId, NetSocket socket, String queueName, boolean noAck) {
        this.consumerTag = consumerTag;
        this.channel = channel;
        this.channelId = channelId;
        this.socket = socket;
        this.queueName = queueName;
        this.noAck = noAck;
        
        logger.info("Created consumer {} for queue {}", consumerTag, queueName);
    }
    
    /**
     * Delivers a message to this consumer.
     *
     * @param messageBody The message body as a string
     * @param exchange The exchange the message was published to
     * @param routingKey The routing key used
     * @param properties Message properties
     */
    public void deliverStringMessage(String messageBody, String exchange, String routingKey, Map<String, Object> properties) {
        try {
            // Generate a delivery tag for this message
            long deliveryTag = DELIVERY_TAG_GENERATOR.getAndIncrement();
            
            // Convert string to bytes
            byte[] bodyBytes = messageBody.getBytes("UTF-8");
            
            // Create and send the Basic.Deliver method frame
            byte[] deliverFrame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.DELIVER)
                .withMethod(AMQPPayloadHeaderType.Basic.DELIVER)
                .withShortStringField("consumer-tag", consumerTag)
                .withLongField("delivery-tag", deliveryTag)
                .withBitField("redelivered", false)
                .withShortStringField("exchange", exchange)
                .withShortStringField("routing-key", routingKey)
                .build();
            
            socket.write(Buffer.buffer(deliverFrame));
            
            // Create and send the content header frame
            byte[] headerFrame = createContentHeaderFrame(bodyBytes.length, properties);
            socket.write(Buffer.buffer(headerFrame));
            
            // Create and send the body frame
            byte[] bodyFrame = createBodyFrame(bodyBytes);
            socket.write(Buffer.buffer(bodyFrame));
            
            logger.info("Delivered message to consumer {}, deliveryTag={}", consumerTag, deliveryTag);
            
            if (noAck) {
                // No acknowledgment needed, consider it acknowledged
                logger.debug("Message automatically acknowledged (noAck=true)");
            }
        } catch (Exception e) {
            logger.error("Error delivering message to consumer " + consumerTag, e);
        }
    }
    
    /**
     * Creates a content header frame.
     *
     * @param bodySize The size of the message body
     * @param properties Message properties
     * @return The content header frame bytes
     * @throws IOException If frame creation fails
     */
    private byte[] createContentHeaderFrame(int bodySize, Map<String, Object> properties) throws IOException {
        // For simplicity, we're just setting content-type to text/plain
        return FrameFactory.headerFrame()
            .withChannel(channelId)
            .withClassId((short) AMQPPayloadHeaderType.Basic.PUBLISH.getClassId())
            .withBodySize(bodySize)
            .withProperty("content-type", "text/plain")
            .withProperty("content-encoding", "utf-8")
            .withProperty("delivery-mode", properties.getOrDefault("delivery-mode", 1))
            .build();
    }
    
    /**
     * Creates a body frame containing the message body.
     *
     * @param bodyBytes The message body bytes
     * @return The body frame bytes
     * @throws IOException If frame creation fails
     */
    private byte[] createBodyFrame(byte[] bodyBytes) throws IOException {
        return FrameFactory.bodyFrame()
            .withChannel(channelId)
            .withContent(bodyBytes)
            .build();
    }
    
    /**
     * Gets the consumer tag.
     *
     * @return The consumer tag
     */
    public String getConsumerTag() {
        return consumerTag;
    }
    
    /**
     * Gets the queue name.
     *
     * @return The queue name
     */
    public String getQueueName() {
        return queueName;
    }
    
    /**
     * Checks if acknowledgment is required.
     *
     * @return True if no acknowledgment required, false otherwise
     */
    public boolean isNoAck() {
        return noAck;
    }
}
