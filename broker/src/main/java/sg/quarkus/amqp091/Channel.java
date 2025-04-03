package sg.quarkus.amqp091;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.broker.BrokerManager;
import sg.quarkus.amqp091.broker.SingleQueueBroker;
import sg.quarkus.amqp091.protocol.frame.AMQPPayloadHeaderType;
import sg.quarkus.amqp091.protocol.frame.FrameFactory;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents an AMQP channel within a connection.
 * Handles channel-level operations according to the AMQP 0-9-1 protocol.
 */
public class Channel {
    private static final Logger logger = LoggerFactory.getLogger(Channel.class);
    
    private final short channelId;
    private final NetSocket socket;
    private final BrokerManager brokerManager; // for future use
    private final SingleQueueBroker simpleBroker = SingleQueueBroker.getInstance();
    private boolean open = false;
    private final AtomicLong deliveryTagCounter = new AtomicLong(0L);
    
    // Store channel-specific properties
    private final Map<String, Object> properties = new HashMap<>();
    
    // Store consumers for this channel
    private final Map<String, Consumer> consumers = new ConcurrentHashMap<>();
    
    // Message handling state
    private boolean expectingContentHeader = false;
    private boolean expectingContentBody = false;
    private int remainingBodySize = 0;
    private int bodyFramesReceived = 0;
    private int contentBodySize = 0;
    private byte[] accumulatedBodyContent = new byte[0];
    private Map<String, Object> currentMessageProperties = new HashMap<>();
    private String currentExchange = "";
    private String currentRoutingKey = "";
    
    /**
     * Creates a new channel with the given ID.
     *
     * @param channelId The channel ID
     * @param socket The socket to use for sending responses
     */
    public Channel(short channelId, NetSocket socket) {
        this.channelId = channelId;
        this.socket = socket;
        this.brokerManager = BrokerManager.getInstance();
        logger.info("Channel {} created", channelId);
    }
    
    /**
     * Handles a frame directed to this channel.
     *
     * @param frameData The raw frame data
     */
    public void handleFrame(byte[] frameData) {
        byte frameType = frameData[0];
        
        if (frameType == 1) { // Method frame
            handleMethodFrame(frameData);
        } else if (frameType == 2) { // Header frame
            handleHeaderFrame(frameData);
        } else if (frameType == 3) { // Body frame
            handleBodyFrame(frameData);
        } else {
            logger.warn("Received unknown frame type {} on channel {}", frameType, channelId);
        }
    }
    
    /**
     * Opens the channel and sends a Channel.Open-Ok response.
     */
    public void open() {
        if (!open) {
            open = true;
            try {
                byte[] frame = FrameFactory.methodFrame()
                    .withChannel(channelId)
                    .withClass(AMQPPayloadHeaderType.Flow.OPEN_OK)
                    .withMethod(AMQPPayloadHeaderType.Flow.OPEN_OK)
                    .withLongStringField("reserved-1", "")
                    .build();
                
                socket.write(Buffer.buffer(frame));
                logger.info("Sent Channel.Open-Ok frame for channel {}", channelId);
            } catch (IOException e) {
                logger.error("Error sending Channel.Open-Ok frame", e);
            }
        }
    }
    
    /**
     * Closes the channel and sends a Channel.Close-Ok response.
     */
    public void close() {
        if (open) {
            open = false;
            try {
                byte[] frame = FrameFactory.methodFrame()
                    .withChannel(channelId)
                    .withClass(AMQPPayloadHeaderType.Flow.CLOSE_OK)
                    .withMethod(AMQPPayloadHeaderType.Flow.CLOSE_OK)
                    .build();
                
                socket.write(Buffer.buffer(frame));
                logger.info("Sent Channel.Close-Ok frame for channel {}", channelId);
            } catch (IOException e) {
                logger.error("Error sending Channel.Close-Ok frame", e);
            }
        }
    }
    
    /**
     * Handles an AMQP method frame.
     *
     * @param frameData The raw frame data
     */
    private void handleMethodFrame(byte[] frameData) {
        short classId = (short) ((frameData[7] & 0xff) << 8 | (frameData[8] & 0xff));
        short methodId = (short) ((frameData[9] & 0xff) << 8 | (frameData[10] & 0xff));
        
        logger.debug("Handling method frame on channel {}: class={}, method={}", channelId, classId, methodId);
        
        // Reset message state if this is a new method frame unrelated to content
        if (classId != 60 || methodId != 40) { // Not Basic.Publish
            resetMessageState();
        }
        
        // Handle different method frames based on class ID
        switch (classId) {
            case 20: // Channel class
                handleChannelMethods(methodId, frameData);
                break;
            case 40: // Exchange class
                handleExchangeMethods(methodId, frameData);
                break;
            case 50: // Queue class
                handleQueueMethods(methodId, frameData);
                break;
            case 60: // Basic class
                handleBasicMethods(methodId, frameData);
                break;
            case 90: // Tx class
                handleTxMethods(methodId, frameData);
                break;
            default:
                logger.warn("Unhandled class ID {} on channel {}", classId, channelId);
        }
    }
    
    /**
     * Handles Channel class methods.
     */
    private void handleChannelMethods(short methodId, byte[] frameData) {
        switch (methodId) {
            case 10: // Channel.Open
                open();
                break;
            case 20: // Channel.Flow
                // Simple implementation, always allow flow
                sendFlowOk(true);
                break;
            case 40: // Channel.Close
                close();
                break;
            default:
                logger.warn("Unhandled Channel method ID {} on channel {}", methodId, channelId);
        }
    }
    
    /**
     * Handles Exchange class methods.
     * This is a simplified implementation that acknowledges operations
     * but doesn't fully implement them.
     */
    private void handleExchangeMethods(short methodId, byte[] frameData) {
        switch (methodId) {
            case 10: // Exchange.Declare
                sendExchangeDeclareOk();
                break;
            case 20: // Exchange.Delete
                sendExchangeDeleteOk();
                break;
            default:
                logger.warn("Unhandled Exchange method ID {} on channel {}", methodId, channelId);
        }
    }
    
    /**
     * Handles Queue class methods.
     * This is a simplified implementation that acknowledges operations
     * but doesn't fully implement them.
     */
    private void handleQueueMethods(short methodId, byte[] frameData) {
        switch (methodId) {
            case 10: // Queue.Declare
                //brokerManager.declareQueue("queue_a", true, false, false, new HashMap<>());
                sendQueueDeclareOk("queue-name", 0, 0);
                break;
            case 20: // Queue.Bind
                sendQueueBindOk();
                break;
            case 30: // Queue.Purge
                sendQueuePurgeOk(0);
                break;
            case 40: // Queue.Delete
                sendQueueDeleteOk(0);
                break;
            case 50: // Queue.Unbind
                sendQueueUnbindOk();
                break;
            default:
                logger.warn("Unhandled Queue method ID {} on channel {}", methodId, channelId);
        }
    }
    
    /**
     * Handles Basic class methods.
     * This is a simplified implementation that acknowledges operations
     * but doesn't fully implement them.
     */
    private void handleBasicMethods(short methodId, byte[] frameData) {
        switch (methodId) {
            case 10: // Basic.Qos
                sendBasicQosOk();
                break;
            case 20: // Basic.Consume
                handleBasicConsume(frameData);
                break;
            case 30: // Basic.Cancel
                sendBasicCancelOk("consumer-tag");
                break;
            case 40: // Basic.Publish
                handleBasicPublish(frameData);
                break;
            case 70: // Basic.Get
                sendBasicGetEmpty("cluster-id");
                break;
            case 80: // Basic.Ack
                // Handle ack
                logger.info("Basic.Ack received on channel {}", channelId);
                break;
            case 90: // Basic.Reject
                // Handle reject
                logger.info("Basic.Reject received on channel {}", channelId);
                break;
            case 110: // Basic.Recover
                sendBasicRecoverOk();
                break;
            default:
                logger.warn("Unhandled Basic method ID {} on channel {}", methodId, channelId);
        }
    }
    
    /**
     * Handles Tx class methods.
     * This is a simplified implementation that acknowledges operations
     * but doesn't fully implement them.
     */
    private void handleTxMethods(short methodId, byte[] frameData) {
        switch (methodId) {
            case 10: // Tx.Select
                sendTxSelectOk();
                break;
            case 20: // Tx.Commit
                sendTxCommitOk();
                break;
            case 30: // Tx.Rollback
                sendTxRollbackOk();
                break;
            default:
                logger.warn("Unhandled Tx method ID {} on channel {}", methodId, channelId);
        }
    }
    
    /**
     * Handles an AMQP header frame.
     * Stores the content properties for the message being published.
     */
    private void handleHeaderFrame(byte[] frameData) {
        logger.info("Header frame received on channel {}", channelId);
        
        if (!expectingContentHeader) {
            logger.warn("Received unexpected header frame on channel {}", channelId);
            return;
        }
        
        // Extract class ID
        short classId = (short) ((frameData[7] & 0xff) << 8 | (frameData[8] & 0xff));
        
        // Extract content body size (8 bytes at offset 11)
        long bodySize = 0;
        for (int i = 0; i < 8; i++) {
            bodySize = (bodySize << 8) | (frameData[11 + i] & 0xff);
        }
        
        contentBodySize = (int) bodySize; // Truncating to int - not ideal for very large messages
        remainingBodySize = contentBodySize;
        logger.debug("Content body size: {} bytes", contentBodySize);
        
        // Extract basic properties from the header frame
        extractContentProperties(frameData);
        
        expectingContentHeader = false;
        expectingContentBody = remainingBodySize > 0;
        accumulatedBodyContent = new byte[contentBodySize];
        bodyFramesReceived = 0;
    }
    
    /**
     * Handles an AMQP body frame.
     * Accumulates message body data until the complete message is received.
     */
    private void handleBodyFrame(byte[] frameData) {
        logger.debug("Body frame received on channel {}", channelId);
        
        if (!expectingContentBody) {
            logger.warn("Received unexpected body frame on channel {}", channelId);
            return;
        }
        
        // Calculate payload size (frame size without the frame header and end byte)
        int frameSize = ((frameData[3] & 0xff) << 24) | 
                       ((frameData[4] & 0xff) << 16) | 
                       ((frameData[5] & 0xff) << 8) | 
                       (frameData[6] & 0xff);
        
        // Copy body frame content into accumulated content
        int copySize = Math.min(frameSize, remainingBodySize);
        System.arraycopy(frameData, 7, accumulatedBodyContent, bodyFramesReceived, copySize);
        bodyFramesReceived += copySize;
        remainingBodySize -= copySize;
        
        logger.debug("Accumulated {} of {} bytes", bodyFramesReceived, contentBodySize);
        
        // Check if we've received the complete message
        if (remainingBodySize <= 0) {
            expectingContentBody = false;
            
            // Process the complete message
            processCompleteMessage();
            
            // Reset state for the next message
            resetMessageState();
        }
    }
    
    // Helper methods to send responses
    
    private void sendFlowOk(boolean active) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Flow.FLOW_OK)
                .withMethod(AMQPPayloadHeaderType.Flow.FLOW_OK)
                .withBitField("active", active)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Channel.Flow-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Channel.Flow-Ok frame", e);
        }
    }
    
    private void sendExchangeDeclareOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Exchange.DECLARE_OK)
                .withMethod(AMQPPayloadHeaderType.Exchange.DECLARE_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Exchange.Declare-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Exchange.Declare-Ok frame", e);
        }
    }
    
    private void sendExchangeDeleteOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Exchange.DELETE_OK)
                .withMethod(AMQPPayloadHeaderType.Exchange.DELETE_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Exchange.Delete-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Exchange.Delete-Ok frame", e);
        }
    }
    
    private void sendQueueDeclareOk(String queueName, int messageCount, int consumerCount) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Queue.DECLARE_OK)
                .withMethod(AMQPPayloadHeaderType.Queue.DECLARE_OK)
                .withShortStringField("queue", queueName)
                .withIntField("message-count", messageCount)
                .withIntField("consumer-count", consumerCount)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Queue.Declare-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Queue.Declare-Ok frame", e);
        }
    }
    
    private void sendQueueBindOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Queue.BIND_OK)
                .withMethod(AMQPPayloadHeaderType.Queue.BIND_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Queue.Bind-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Queue.Bind-Ok frame", e);
        }
    }
    
    private void sendQueueUnbindOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Queue.UNBIND_OK)
                .withMethod(AMQPPayloadHeaderType.Queue.UNBIND_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Queue.Unbind-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Queue.Unbind-Ok frame", e);
        }
    }
    
    private void sendQueuePurgeOk(int messageCount) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Queue.PURGE_OK)
                .withMethod(AMQPPayloadHeaderType.Queue.PURGE_OK)
                .withIntField("message-count", messageCount)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Queue.Purge-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Queue.Purge-Ok frame", e);
        }
    }
    
    private void sendQueueDeleteOk(int messageCount) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Queue.DELETE_OK)
                .withMethod(AMQPPayloadHeaderType.Queue.DELETE_OK)
                .withIntField("message-count", messageCount)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Queue.Delete-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Queue.Delete-Ok frame", e);
        }
    }
    
    private void sendBasicQosOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.QOS_OK)
                .withMethod(AMQPPayloadHeaderType.Basic.QOS_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Basic.Qos-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Basic.Qos-Ok frame", e);
        }
    }
    
    private void sendBasicConsumeOk(String consumerTag) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.CONSUME_OK)
                .withMethod(AMQPPayloadHeaderType.Basic.CONSUME_OK)
                .withShortStringField("consumer-tag", consumerTag)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Basic.Consume-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Basic.Consume-Ok frame", e);
        }
    }
    
    private void sendBasicCancelOk(String consumerTag) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.CANCEL_OK)
                .withMethod(AMQPPayloadHeaderType.Basic.CANCEL_OK)
                .withShortStringField("consumer-tag", consumerTag)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Basic.Cancel-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Basic.Cancel-Ok frame", e);
        }
    }
    
    private void sendBasicGetEmpty(String clusterId) {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.GET_EMPTY)
                .withMethod(AMQPPayloadHeaderType.Basic.GET_EMPTY)
                .withShortStringField("cluster-id", clusterId)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Basic.GetEmpty frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Basic.GetEmpty frame", e);
        }
    }
    
    private void sendBasicRecoverOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Basic.RECOVER_OK)
                .withMethod(AMQPPayloadHeaderType.Basic.RECOVER_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Basic.Recover-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Basic.Recover-Ok frame", e);
        }
    }
    
    private void sendTxSelectOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Tx.SELECT_OK)
                .withMethod(AMQPPayloadHeaderType.Tx.SELECT_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Tx.Select-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Tx.Select-Ok frame", e);
        }
    }
    
    private void sendTxCommitOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Tx.COMMIT_OK)
                .withMethod(AMQPPayloadHeaderType.Tx.COMMIT_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Tx.Commit-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Tx.Commit-Ok frame", e);
        }
    }
    
    private void sendTxRollbackOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel(channelId)
                .withClass(AMQPPayloadHeaderType.Tx.ROLLBACK_OK)
                .withMethod(AMQPPayloadHeaderType.Tx.ROLLBACK_OK)
                .build();
            
            socket.write(Buffer.buffer(frame));
            logger.info("Sent Tx.Rollback-Ok frame for channel {}", channelId);
        } catch (IOException e) {
            logger.error("Error sending Tx.Rollback-Ok frame", e);
        }
    }
    
    /**
     * @return The channel ID
     */
    public short getChannelId() {
        return channelId;
    }
    
    /**
     * @return True if the channel is open, false otherwise
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Handles a Basic.Publish method frame.
     * Sets up the state for receiving content header and body frames.
     */
    private void handleBasicPublish(byte[] frameData) {
        logger.info("Basic.Publish received on channel {}", channelId);

        // Parse the Basic.Publish frame to extract exchange and routing key
        // Offset 11 is where the method arguments start
        int offset = 11;

        // Skip ticket (short)
        offset += 2;

        // Extract exchange name (short string)
        int exchangeNameLength = frameData[offset++] & 0xff;
        byte[] exchangeNameBytes = new byte[exchangeNameLength];
        System.arraycopy(frameData, offset, exchangeNameBytes, 0, exchangeNameLength);
        currentExchange = new String(exchangeNameBytes);
        offset += exchangeNameLength;

        // Extract routing key (short string)
        int routingKeyLength = frameData[offset++] & 0xff;
        byte[] routingKeyBytes = new byte[routingKeyLength];
        System.arraycopy(frameData, offset, routingKeyBytes, 0, routingKeyLength);
        currentRoutingKey = new String(routingKeyBytes);

        logger.debug("Publishing to exchange: '{}' with routing key: '{}'", currentExchange, currentRoutingKey);

        // Set up state for receiving content header and body
        expectingContentHeader = true;
        currentMessageProperties.clear();
    }

    /**
     * Extracts content properties from a header frame.
     * This is a simplified implementation focusing on key properties.
     */
    private void extractContentProperties(byte[] frameData) {
        // In a real implementation, this would parse all the content properties
        // For simplicity, we're just storing minimal properties
        currentMessageProperties.put("content-type", "application/octet-stream");
        currentMessageProperties.put("delivery-mode", 1); // Non-persistent
    }

    /**
    * Processes a complete message after all frames have been received.
    */
    private void processCompleteMessage() {
        logger.info("Complete message received on channel {}, size: {} bytes",
                channelId, contentBodySize);

        try {
            // For debugging, show the message content
            String content = new String(accumulatedBodyContent, StandardCharsets.UTF_8);

            // Use the broker to publish the message
            SingleQueueBroker.getInstance().publishMessage(
                    accumulatedBodyContent,
                    currentMessageProperties,
                    currentExchange,
                    currentRoutingKey
            );

            logger.info("Message published successfully: '{}'", content);
        } catch (Exception e) {
            logger.error("Error publishing message", e);
        }
    }

    /**
     * Resets the message state for the next message.
     */
    private void resetMessageState() {
        expectingContentHeader = false;
        expectingContentBody = false;
        remainingBodySize = 0;
        bodyFramesReceived = 0;
        contentBodySize = 0;
        accumulatedBodyContent = new byte[0];
        currentMessageProperties.clear();
        currentExchange = "";
        currentRoutingKey = "";
    }

    /**
     * Handles a Basic.Consume method frame.
     * Sets up a consumer for receiving messages from a queue.
     */
    private void handleBasicConsume(byte[] frameData) {
        logger.info("Basic.Consume received on channel {}", channelId);

        try {
            // Extract queue name and consumer tag (simplified extraction)
            int offset = 11; // Skip method header
            offset += 2; // Skip ticket

            // Extract queue name
            int queueNameLength = frameData[offset++] & 0xFF;
            byte[] queueNameBytes = new byte[queueNameLength];
            System.arraycopy(frameData, offset, queueNameBytes, 0, queueNameLength);
            String queueName = new String(queueNameBytes);
            offset += queueNameLength;

            // Extract consumer tag
            int consumerTagLength = frameData[offset++] & 0xFF;
            String consumerTag = "";
            if (consumerTagLength > 0) {
                byte[] tagBytes = new byte[consumerTagLength];
                System.arraycopy(frameData, offset, tagBytes, 0, consumerTagLength);
                consumerTag = new String(tagBytes);
            }

            // Register with SingleQueueBroker, passing this channel as the handler
            String actualTag = SingleQueueBroker.getInstance().registerConsumer(
                    queueName, consumerTag, this);

            // Send Basic.Consume-Ok
            sendBasicConsumeOk(actualTag);

            logger.info("Consumer {} created for queue {}", actualTag, queueName);
        } catch (Exception e) {
            logger.error("Error handling Basic.Consume", e);
        }
    }


    public void deliverMessageWithActualTag(String consumerTag, byte[] body) {
        try {
            // Convert the actual consumer tag to bytes
            byte[] tagBytes = consumerTag.getBytes(StandardCharsets.UTF_8);
            int tagLength = tagBytes.length;

            // 1. Calculate Method Frame Size and create the frame
            int methodPayloadSize = 4 + 1 + tagLength + 8 + 1 + 1 + 1; // class+method + tag length + tag + delivery tag + redelivered + exchange + routing key
            int methodFrameSize = 7 + methodPayloadSize + 1; // header + payload + end marker

            byte[] methodFrame = new byte[methodFrameSize];
            int offset = 0;

            // Frame header
            methodFrame[offset++] = 1;          // frame type: method
            methodFrame[offset++] = 0;          // channel MSB
            methodFrame[offset++] = 1;          // channel LSB

            // Frame size (4 bytes)
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = (byte)((methodPayloadSize >> 8) & 0xFF);
            methodFrame[offset++] = (byte)(methodPayloadSize & 0xFF);

            // Class and method (4 bytes)
            methodFrame[offset++] = 0;          // class MSB (Basic = 60)
            methodFrame[offset++] = 60;         // class LSB
            methodFrame[offset++] = 0;          // method MSB (Deliver = 60)
            methodFrame[offset++] = 60;         // method LSB

            // Consumer tag
            methodFrame[offset++] = (byte)tagLength;
            System.arraycopy(tagBytes, 0, methodFrame, offset, tagLength);
            offset += tagLength;

            // Delivery tag: 1
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 0;
            methodFrame[offset++] = 1;

            // Redelivered: false
            methodFrame[offset++] = 0;

            // Exchange: empty string
            methodFrame[offset++] = 0;

            // Routing key: empty string
            methodFrame[offset++] = 0;

            // Frame end
            methodFrame[offset] = (byte)0xCE;

            // Verify correct size calculation
            logger.debug("Method frame size: payload={}, total={}, actual={}",
                    methodPayloadSize, methodFrameSize, methodFrame.length);

            socket.write(Buffer.buffer(methodFrame));
            logger.debug("Sent method frame: {}", bytesToHex(methodFrame));

            // 2. Header frame (exactly as before)
            byte[] headerFrame = new byte[22];
            headerFrame[0] = 2;          // frame type: header
            headerFrame[1] = 0;          // channel MSB
            headerFrame[2] = 1;          // channel LSB
            headerFrame[3] = 0;          // size MSB
            headerFrame[4] = 0;          // size
            headerFrame[5] = 0;          // size
            headerFrame[6] = 14;         // size LSB (14 bytes of content header)
            headerFrame[7] = 0;          // class-id MSB (Basic = 60)
            headerFrame[8] = 60;         // class-id LSB
            headerFrame[9] = 0;          // weight MSB
            headerFrame[10] = 0;         // weight LSB

            // Body size (8 bytes)
            headerFrame[11] = 0;
            headerFrame[12] = 0;
            headerFrame[13] = 0;
            headerFrame[14] = 0;
            headerFrame[15] = 0;
            headerFrame[16] = 0;
            headerFrame[17] = 0;
            headerFrame[18] = (byte)body.length;

            // Property flags
            headerFrame[19] = 0;
            headerFrame[20] = 0;

            // Frame end
            headerFrame[21] = (byte)0xCE;

            socket.write(Buffer.buffer(headerFrame));
            logger.debug("Sent header frame: {}", bytesToHex(headerFrame));

            // 3. Body frame with correct size
            int bodyFrameSize = 7 + body.length + 1;
            byte[] bodyFrame = new byte[bodyFrameSize];

            offset = 0;
            bodyFrame[offset++] = 3;           // frame type: body
            bodyFrame[offset++] = 0;           // channel MSB
            bodyFrame[offset++] = 1;           // channel LSB

            // Frame size = body length
            bodyFrame[offset++] = 0;
            bodyFrame[offset++] = 0;
            bodyFrame[offset++] = (byte)((body.length >> 8) & 0xFF);
            bodyFrame[offset++] = (byte)(body.length & 0xFF);

            // Body content
            System.arraycopy(body, 0, bodyFrame, offset, body.length);
            offset += body.length;

            // Frame end
            bodyFrame[offset] = (byte)0xCE;

            socket.write(Buffer.buffer(bodyFrame));
            logger.debug("Sent body frame: {}", bytesToHex(bodyFrame));

            logger.info("Delivered message to consumer {} with actual tag", consumerTag);
        } catch (Exception e) {
            logger.error("Error delivering message with actual tag: {}", e.getMessage(), e);
        }
    }

    public void deliverMessageSimplified(String consumerTag, byte[] body, Map<String, Object> properties, String exchange, String routingKey) {
        try {
            // Use a short consumer tag for simplicity
            String shortTag = "tag1";

            // 1. Basic.Deliver method frame (simplified, fixed size)
            byte[] deliverFrame = {
                    0x01,               // Type: method
                    0x00, 0x01,         // Channel: 1
                    0x00, 0x00, 0x00, 0x1D, // Size: 29 bytes
                    0x00, 0x3C,         // Class: 60 (Basic)
                    0x00, 0x3C,         // Method: 60 (Deliver)
                    0x04,               // Tag length: 4
                    't', 'a', 'g', '1', // Tag: "tag1"
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, // Delivery tag: 1
                    0x00,               // Redelivered: false
                    0x00,               // Exchange name length: 0 (empty string)
                    0x00,               // Routing key length: 0 (empty string)
                    (byte)0xCE          // Frame end
            };

            socket.write(Buffer.buffer(deliverFrame));
            logger.debug("Sent Basic.Deliver frame");

            // 2. Content header frame (simplified)
            byte[] headerFrame = {
                    0x02,               // Type: header
                    0x00, 0x01,         // Channel: 1
                    0x00, 0x00, 0x00, 0x0E, // Size: 14 bytes
                    0x00, 0x3C,         // Class: 60 (Basic)
                    0x00, 0x00,         // Weight: 0
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)body.length, // Body size
                    0x00, 0x00,         // Property flags: none
                    (byte)0xCE          // Frame end
            };

            socket.write(Buffer.buffer(headerFrame));
            logger.debug("Sent content header frame");

            // 3. Content body frame
            byte[] bodyFrameHeader = {
                    0x03,               // Type: body
                    0x00, 0x01,         // Channel: 1
                    0x00, 0x00, 0x00, (byte)body.length // Size: body length
            };

            // Combine header + body + end marker
            byte[] bodyFrame = new byte[bodyFrameHeader.length + body.length + 1];
            System.arraycopy(bodyFrameHeader, 0, bodyFrame, 0, bodyFrameHeader.length);
            System.arraycopy(body, 0, bodyFrame, bodyFrameHeader.length, body.length);
            bodyFrame[bodyFrameHeader.length + body.length] = (byte)0xCE; // Frame end

            socket.write(Buffer.buffer(bodyFrame));
            logger.debug("Sent content body frame");

            logger.info("Delivered simplified message to consumer {}", shortTag);
        } catch (Exception e) {
            logger.error("Error delivering simplified message: {}", e.getMessage(), e);
        }
    }

    public void deliverMessageManually(String consumerTag, byte[] body,
                                       Map<String, Object> properties,
                                       String exchange, String routingKey) {
        try {
            // Generate a delivery tag (1 for simplicity)
            long deliveryTag = 1;

            // Convert strings to bytes
            byte[] tagBytes = consumerTag.getBytes(StandardCharsets.UTF_8);
            byte tagLength = (byte) tagBytes.length;

            byte[] exchangeBytes = exchange.getBytes(StandardCharsets.UTF_8);
            byte exchangeLength = (byte) exchangeBytes.length;

            byte[] routingKeyBytes = routingKey.getBytes(StandardCharsets.UTF_8);
            byte routingKeyLength = (byte) routingKeyBytes.length;

            // Calculate method payload size
            int methodPayloadSize = 4 + // class ID + method ID
                    1 + tagBytes.length + // consumer tag
                    8 + // delivery tag
                    1 + // redelivered
                    1 + exchangeBytes.length + // exchange
                    1 + routingKeyBytes.length; // routing key

            // Basic.Deliver frame
            byte[] deliverFrame = new byte[7 + methodPayloadSize + 1]; // 7 bytes header + method payload + 1 frame end

            int offset = 0;

            // Frame header
            deliverFrame[offset++] = 1; // Frame type (method)
            deliverFrame[offset++] = 0; // Channel MSB
            deliverFrame[offset++] = 1; // Channel LSB

            // Frame payload size (4 bytes, big endian)
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = (byte) ((methodPayloadSize >> 8) & 0xFF);
            deliverFrame[offset++] = (byte) (methodPayloadSize & 0xFF);

            // Class ID: Basic (60)
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 60;

            // Method ID: Deliver (60)
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 60;

            // Consumer tag (short string)
            deliverFrame[offset++] = tagLength;
            System.arraycopy(tagBytes, 0, deliverFrame, offset, tagBytes.length);
            offset += tagBytes.length;

            // Delivery tag (8 bytes)
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 0;
            deliverFrame[offset++] = 1; // Value 1

            // Redelivered (1 byte)
            deliverFrame[offset++] = 0;

            // Exchange (short string)
            deliverFrame[offset++] = exchangeLength;
            System.arraycopy(exchangeBytes, 0, deliverFrame, offset, exchangeBytes.length);
            offset += exchangeBytes.length;

            // Routing key (short string)
            deliverFrame[offset++] = routingKeyLength;
            System.arraycopy(routingKeyBytes, 0, deliverFrame, offset, routingKeyBytes.length);
            offset += routingKeyBytes.length;

            // Frame end
            deliverFrame[offset] = (byte) 0xCE;

            // Write the method frame
            socket.write(Buffer.buffer(deliverFrame));
            logger.debug("Sent Basic.Deliver frame, size={}", deliverFrame.length);

            // Content header frame
            int headerPayloadSize = 14; // 2 class ID + 2 weight + 8 body size + 2 property flags
            byte[] headerFrame = new byte[7 + headerPayloadSize + 1];

            offset = 0;
            headerFrame[offset++] = 2; // Frame type (header)
            headerFrame[offset++] = 0; // Channel MSB
            headerFrame[offset++] = 1; // Channel LSB

            // Frame payload size
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 14; // Fixed size for simple header

            // Class ID: Basic (60)
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 60;

            // Weight (0)
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;

            // Body size (8 bytes)
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = 0;
            headerFrame[offset++] = (byte) body.length;

            // Property flags - content-type (bit 15)
            headerFrame[offset++] = (byte) 0x80;
            headerFrame[offset++] = 0;

            // Frame end
            headerFrame[offset] = (byte) 0xCE;

            // Write the header frame
            socket.write(Buffer.buffer(headerFrame));
            logger.debug("Sent content header frame, size={}", headerFrame.length);

            // Content body frame
            int bodyFrameSize = 7 + body.length + 1;
            byte[] bodyFrame = new byte[bodyFrameSize];

            offset = 0;
            bodyFrame[offset++] = 3; // Frame type (body)
            bodyFrame[offset++] = 0; // Channel MSB
            bodyFrame[offset++] = 1; // Channel LSB

            // Frame payload size
            bodyFrame[offset++] = 0;
            bodyFrame[offset++] = 0;
            bodyFrame[offset++] = 0;
            bodyFrame[offset++] = (byte) body.length;

            // Body content
            System.arraycopy(body, 0, bodyFrame, offset, body.length);
            offset += body.length;

            // Frame end
            bodyFrame[offset] = (byte) 0xCE;

            // Write the body frame
            socket.write(Buffer.buffer(bodyFrame));
            logger.debug("Sent content body frame, size={}", bodyFrame.length);

            logger.info("Manually delivered message to consumer {}", consumerTag);
        } catch (Exception e) {
            logger.error("Error manually delivering message: {}", e.getMessage(), e);
        }
    }

    public void deliverMessage(String consumerTag, byte[] body,
                               Map<String, Object> properties,
                               String exchange, String routingKey) {
        try {
            // Generate a delivery tag
            long deliveryTag = deliveryTagCounter.incrementAndGet(); // Use an AtomicLong for this

            // 1. Send Basic.Deliver method frame
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

            logger.debug("Sending Basic.Deliver frame: {}", bytesToHex(deliverFrame));
            socket.write(Buffer.buffer(deliverFrame));

            // 2. Send content header frame - properly formatted for Basic class
            // Create a more complete header frame
            byte[] headerFrame = FrameFactory.headerFrame()
                    .withChannel(channelId)
                    .withClassId((short) 60) // Basic class ID
                    .withBodySize(body.length)
                    // Explicitly set important properties with non-null values
                    .withProperty("content-type", "text/plain")
                    .withProperty("content-encoding", "utf-8")
                    .withProperty("delivery-mode", 1) // Non-persistent
                    .withProperty("priority", 0)      // Default priority
                    .withProperty("timestamp", new Date()) // Current time
                    .build();

            logger.debug("Sending Content Header frame: {}", bytesToHex(headerFrame));
            socket.write(Buffer.buffer(headerFrame));

            // 3. Send content body frame(s)
            // For large messages, you'd split this into multiple frames
            byte[] bodyFrame = FrameFactory.bodyFrame()
                    .withChannel(channelId)
                    .withContent(body)
                    .build();

            logger.debug("Sending Content Body frame: {}", bytesToHex(bodyFrame));
            socket.write(Buffer.buffer(bodyFrame));

            logger.info("Delivered message to consumer {}, content: {}",
                    consumerTag, new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            logger.error("Error delivering message", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    /**
     * Delivers a message to a consumer on this channel.
     *
     * @param consumerTag The consumer tag
     * @param message The message to deliver
     * @return True if delivered successfully, false otherwise
     */
    public boolean deliverStringMessage(String consumerTag, StringMessage message) {
        Consumer consumer = consumers.get(consumerTag);
        if (consumer == null) {
            logger.warn("Cannot deliver message: consumer {} not found", consumerTag);
            return false;
        }

        consumer.deliverStringMessage(
                message.getContent(),
                message.getExchange(),
                message.getRoutingKey(),
                message.getProperties()
        );

        return true;
    }
}
