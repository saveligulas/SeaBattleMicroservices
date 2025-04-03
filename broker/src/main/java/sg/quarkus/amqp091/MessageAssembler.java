package sg.quarkus.amqp091;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the state for assembling multi-frame AMQP messages.
 * Handles the sequence of method frame + header frame + body frame(s).
 */
public class MessageAssembler {
    private static final Logger logger = LoggerFactory.getLogger(MessageAssembler.class);
    
    // Message assembly state
    private boolean expectingContentHeader = false;
    private boolean expectingContentBody = false;
    private int remainingBodySize = 0;
    private int bodyAccumulatedSize = 0;
    private int contentBodySize = 0;
    private byte[] accumulatedBodyContent = new byte[0];
    
    // Message properties and routing information
    private final Map<String, Object> messageProperties = new HashMap<>();
    private String exchange = "";
    private String routingKey = "";
    
    /**
     * Resets the message assembly state.
     */
    public void reset() {
        expectingContentHeader = false;
        expectingContentBody = false;
        remainingBodySize = 0;
        bodyAccumulatedSize = 0;
        contentBodySize = 0;
        accumulatedBodyContent = new byte[0];
        messageProperties.clear();
        exchange = "";
        routingKey = "";
    }
    
    /**
     * Prepares the assembler for receiving a content header after a Basic.Publish.
     * 
     * @param exchange The exchange to publish to
     * @param routingKey The routing key for the message
     */
    public void expectHeader(String exchange, String routingKey) {
        reset();
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.expectingContentHeader = true;
        logger.debug("Ready to receive content header for message to exchange={}, routingKey={}", exchange, routingKey);
    }
    
    /**
     * Processes a content header frame and prepares for body frames.
     * 
     * @param contentBodySize The size of the content body in bytes
     * @param properties The message properties
     * @return True if ready for body frames, false if there's no body (zero size)
     */
    public boolean processHeader(int contentBodySize, Map<String, Object> properties) {
        if (!expectingContentHeader) {
            logger.warn("Received unexpected content header");
            return false;
        }
        
        this.contentBodySize = contentBodySize;
        this.remainingBodySize = contentBodySize;
        this.messageProperties.putAll(properties);
        
        logger.debug("Processed content header, body size = {} bytes", contentBodySize);
        
        expectingContentHeader = false;
        
        if (contentBodySize > 0) {
            expectingContentBody = true;
            accumulatedBodyContent = new byte[contentBodySize];
            return true;
        } else {
            // No body to expect
            expectingContentBody = false;
            return false;
        }
    }
    
    /**
     * Processes a body frame by accumulating its content.
     * 
     * @param bodyContent The body frame content (payload only)
     * @param bodySize The size of this body frame portion
     * @return True if the message is complete, false if more body frames are expected
     */
    public boolean processBodyFrame(byte[] bodyContent, int bodySize) {
        if (!expectingContentBody) {
            logger.warn("Received unexpected body frame");
            return false;
        }
        
        int copySize = Math.min(bodySize, remainingBodySize);
        System.arraycopy(bodyContent, 0, accumulatedBodyContent, bodyAccumulatedSize, copySize);
        bodyAccumulatedSize += copySize;
        remainingBodySize -= copySize;
        
        logger.debug("Added {} bytes to body, {}/{} bytes accumulated", 
                copySize, bodyAccumulatedSize, contentBodySize);
        
        if (remainingBodySize <= 0) {
            expectingContentBody = false;
            return true; // Message is complete
        }
        
        return false; // More body frames expected
    }
    
    /**
     * Checks if the assembler is waiting for a content header.
     */
    public boolean isExpectingHeader() {
        return expectingContentHeader;
    }
    
    /**
     * Checks if the assembler is waiting for a body frame.
     */
    public boolean isExpectingBody() {
        return expectingContentBody;
    }
    
    /**
     * Checks if the message assembly process is complete.
     */
    public boolean isComplete() {
        return !expectingContentHeader && !expectingContentBody && (remainingBodySize <= 0);
    }
    
    /**
     * Gets the assembled message properties.
     */
    public Map<String, Object> getMessageProperties() {
        return new HashMap<>(messageProperties);
    }
    
    /**
     * Gets the destination exchange for the message.
     */
    public String getExchange() {
        return exchange;
    }
    
    /**
     * Gets the routing key for the message.
     */
    public String getRoutingKey() {
        return routingKey;
    }
    
    /**
     * Gets the accumulated body content.
     * Should only be called when the message is complete.
     */
    public byte[] getBodyContent() {
        return accumulatedBodyContent;
    }
    
    /**
     * Gets the total body size.
     */
    public int getBodySize() {
        return contentBodySize;
    }
}
