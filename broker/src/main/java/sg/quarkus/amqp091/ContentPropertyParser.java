package sg.quarkus.amqp091;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for AMQP content properties from header frames.
 * Based on AMQP 0-9-1 specification for basic content properties.
 */
public class ContentPropertyParser {
    private static final Logger logger = LoggerFactory.getLogger(ContentPropertyParser.class);
    
    // Property flags are 2 bytes at offset 19 in the header frame
    private static final int PROPERTY_FLAGS_OFFSET = 19;
    
    // Property bits in the property flags field
    private static final int CONTENT_TYPE_BIT = 15;
    private static final int CONTENT_ENCODING_BIT = 14;
    private static final int HEADERS_BIT = 13;
    private static final int DELIVERY_MODE_BIT = 12;
    private static final int PRIORITY_BIT = 11;
    private static final int CORRELATION_ID_BIT = 10;
    private static final int REPLY_TO_BIT = 9;
    private static final int EXPIRATION_BIT = 8;
    private static final int MESSAGE_ID_BIT = 7;
    private static final int TIMESTAMP_BIT = 6;
    private static final int TYPE_BIT = 5;
    private static final int USER_ID_BIT = 4;
    private static final int APP_ID_BIT = 3;
    private static final int RESERVED_BIT = 2;
    
    /**
     * Parses content properties from a header frame.
     *
     * @param frameData The raw header frame data
     * @return A map of property names to values
     */
    public static Map<String, Object> parseProperties(byte[] frameData) {
        Map<String, Object> properties = new HashMap<>();
        
        try {
            // Extract property flags (2 bytes)
            int propertyFlags = ((frameData[PROPERTY_FLAGS_OFFSET] & 0xff) << 8) | 
                             (frameData[PROPERTY_FLAGS_OFFSET + 1] & 0xff);
            
            int offset = PROPERTY_FLAGS_OFFSET + 2; // Start after property flags
            
            // Check and parse each property based on property flags
            if (isBitSet(propertyFlags, CONTENT_TYPE_BIT)) {
                offset = parseShortString(frameData, offset, "content-type", properties);
            }
            
            if (isBitSet(propertyFlags, CONTENT_ENCODING_BIT)) {
                offset = parseShortString(frameData, offset, "content-encoding", properties);
            }
            
            if (isBitSet(propertyFlags, HEADERS_BIT)) {
                offset = parseFieldTable(frameData, offset, "headers", properties);
            }
            
            if (isBitSet(propertyFlags, DELIVERY_MODE_BIT)) {
                properties.put("delivery-mode", frameData[offset++] & 0xff);
            }
            
            if (isBitSet(propertyFlags, PRIORITY_BIT)) {
                properties.put("priority", frameData[offset++] & 0xff);
            }
            
            if (isBitSet(propertyFlags, CORRELATION_ID_BIT)) {
                offset = parseShortString(frameData, offset, "correlation-id", properties);
            }
            
            if (isBitSet(propertyFlags, REPLY_TO_BIT)) {
                offset = parseShortString(frameData, offset, "reply-to", properties);
            }
            
            if (isBitSet(propertyFlags, EXPIRATION_BIT)) {
                offset = parseShortString(frameData, offset, "expiration", properties);
            }
            
            if (isBitSet(propertyFlags, MESSAGE_ID_BIT)) {
                offset = parseShortString(frameData, offset, "message-id", properties);
            }
            
            if (isBitSet(propertyFlags, TIMESTAMP_BIT)) {
                long timestamp = 0;
                for (int i = 0; i < 8; i++) {
                    timestamp = (timestamp << 8) | (frameData[offset++] & 0xff);
                }
                properties.put("timestamp", new Date(timestamp * 1000)); // Convert to milliseconds
            }
            
            if (isBitSet(propertyFlags, TYPE_BIT)) {
                offset = parseShortString(frameData, offset, "type", properties);
            }
            
            if (isBitSet(propertyFlags, USER_ID_BIT)) {
                offset = parseShortString(frameData, offset, "user-id", properties);
            }
            
            if (isBitSet(propertyFlags, APP_ID_BIT)) {
                offset = parseShortString(frameData, offset, "app-id", properties);
            }
            
            // Reserved bit - should not be set
            if (isBitSet(propertyFlags, RESERVED_BIT)) {
                logger.warn("Reserved property bit is set in content properties");
            }
            
            logger.debug("Parsed {} content properties", properties.size());
        } catch (Exception e) {
            logger.error("Error parsing content properties", e);
            
            // Set default properties on error
            properties.clear();
            properties.put("content-type", "application/octet-stream");
            properties.put("delivery-mode", 1); // Non-persistent
        }
        
        return properties;
    }
    
    /**
     * Checks if a specific bit is set in a bit field.
     *
     * @param bits The bit field
     * @param position The bit position (0-15, where 15 is the most significant bit)
     * @return True if the bit is set, false otherwise
     */
    private static boolean isBitSet(int bits, int position) {
        return ((bits >> position) & 1) == 1;
    }
    
    /**
     * Parses a short string (length byte + string bytes) from the frame data.
     *
     * @param frameData The frame data
     * @param offset The current offset in the frame data
     * @param propertyName The property name to store in the properties map
     * @param properties The properties map to store the parsed value in
     * @return The new offset after parsing the string
     */
    private static int parseShortString(byte[] frameData, int offset, String propertyName, Map<String, Object> properties) {
        int length = frameData[offset++] & 0xff;
        if (length > 0) {
            byte[] stringBytes = new byte[length];
            System.arraycopy(frameData, offset, stringBytes, 0, length);
            properties.put(propertyName, new String(stringBytes, StandardCharsets.UTF_8));
        } else {
            properties.put(propertyName, "");
        }
        return offset + length;
    }
    
    /**
     * Parses a field table (length + table data) from the frame data.
     * This is a simplified implementation that only handles basic types.
     *
     * @param frameData The frame data
     * @param offset The current offset in the frame data
     * @param propertyName The property name to store in the properties map
     * @param properties The properties map to store the parsed value in
     * @return The new offset after parsing the table
     */
    private static int parseFieldTable(byte[] frameData, int offset, String propertyName, Map<String, Object> properties) {
        // Get table size (4 bytes)
        int tableSize = ((frameData[offset] & 0xff) << 24) | 
                         ((frameData[offset + 1] & 0xff) << 16) | 
                         ((frameData[offset + 2] & 0xff) << 8) | 
                         (frameData[offset + 3] & 0xff);
        offset += 4;
        
        Map<String, Object> table = new HashMap<>();
        
        // Read table fields until we've consumed tableSize bytes
        int endOffset = offset + tableSize;
        while (offset < endOffset) {
            // Read field name (short string)
            int nameLength = frameData[offset++] & 0xff;
            byte[] nameBytes = new byte[nameLength];
            System.arraycopy(frameData, offset, nameBytes, 0, nameLength);
            String fieldName = new String(nameBytes, StandardCharsets.UTF_8);
            offset += nameLength;
            
            // Read field value type
            byte fieldType = frameData[offset++];
            
            // Parse field value based on type
            switch (fieldType) {
                case 't': // Boolean
                    table.put(fieldName, frameData[offset++] != 0);
                    break;
                case 'b': // Signed 8-bit
                    table.put(fieldName, (byte) frameData[offset++]);
                    break;
                case 's': // Signed 16-bit
                    table.put(fieldName, (short) ((frameData[offset++] & 0xff) << 8 | 
                                                 (frameData[offset++] & 0xff)));
                    break;
                case 'I': // Signed 32-bit
                    table.put(fieldName, ((frameData[offset++] & 0xff) << 24) | 
                                        ((frameData[offset++] & 0xff) << 16) | 
                                        ((frameData[offset++] & 0xff) << 8) | 
                                        (frameData[offset++] & 0xff));
                    break;
                case 'f': // 32-bit float
                    int floatBits = ((frameData[offset++] & 0xff) << 24) | 
                                   ((frameData[offset++] & 0xff) << 16) | 
                                   ((frameData[offset++] & 0xff) << 8) | 
                                   (frameData[offset++] & 0xff);
                    table.put(fieldName, Float.intBitsToFloat(floatBits));
                    break;
                case 'S': // Long string
                    int strLength = ((frameData[offset++] & 0xff) << 24) | 
                                   ((frameData[offset++] & 0xff) << 16) | 
                                   ((frameData[offset++] & 0xff) << 8) | 
                                   (frameData[offset++] & 0xff);
                    byte[] strBytes = new byte[strLength];
                    System.arraycopy(frameData, offset, strBytes, 0, strLength);
                    table.put(fieldName, new String(strBytes, StandardCharsets.UTF_8));
                    offset += strLength;
                    break;
                default:
                    logger.warn("Unsupported field type '{}' in table, skipping", (char) fieldType);
                    // Skip the rest of the table since we don't know how to parse this field
                    offset = endOffset;
                    break;
            }
        }
        
        properties.put(propertyName, table);
        return offset;
    }
}
