package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;
import sg.quarkus.amqp091.protocol.frame.data.AMQPDataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A utility class for building AMQP frames with a more fluent API.
 * This class simplifies frame creation, particularly when dealing with both 
 * class and method types that implement multiple interfaces.
 */
public class FrameBuilder {
    
    /**
     * Creates a method frame for a type that implements both IFramePayloadClass and IFramePayloadMethod.
     * This is a convenience method for creating frames from AMQPPayloadHeaderType enum values.
     * 
     * @param <T> Type that implements both IFramePayloadClass and IFramePayloadMethod
     * @param channel The channel number
     * @param type The type implementing both interfaces
     * @return Byte array representing the method frame
     * @throws IOException If frame creation fails
     */
    public static <T extends IFramePayloadClass & IFramePayloadMethod> byte[] createMethodFrame(
            short channel, T type) throws IOException {
        return FrameFactory.methodFrame()
                .withChannel(channel)
                .withClass(type)
                .withMethod(type)
                .build();
    }
    
    /**
     * Creates a method frame with data fields for a type that implements both IFramePayloadClass and IFramePayloadMethod.
     * 
     * @param <T> Type that implements both IFramePayloadClass and IFramePayloadMethod
     * @param channel The channel number
     * @param type The type implementing both interfaces
     * @param fields List of data fields to include in the frame
     * @return Byte array representing the method frame
     * @throws IOException If frame creation fails
     */
    public static <T extends IFramePayloadClass & IFramePayloadMethod> byte[] createMethodFrame(
            short channel, T type, List<AMQPDataField> fields) throws IOException {
        
        FrameFactory.MethodFrameBuilder builder = FrameFactory.methodFrame()
                .withChannel(channel)
                .withClass(type)
                .withMethod(type);
        
        // Add all fields to the builder
        for (AMQPDataField field : fields) {
            switch (field.getType()) {
                case BIT:
                    builder.withBitField(field.getName(), (Boolean) field.getValue());
                    break;
                case OCTET:
                    builder.withOctetField(field.getName(), (Byte) field.getValue());
                    break;
                case SHORT:
                    builder.withShortField(field.getName(), (Short) field.getValue());
                    break;
                case INT:
                    builder.withIntField(field.getName(), (Integer) field.getValue());
                    break;
                case LONG:
                    if (field.getValue() instanceof Integer) {
                        builder.withIntField(field.getName(), (Integer) field.getValue());
                    } else {
                        builder.withLongField(field.getName(), ((Number) field.getValue()).longValue());
                    }
                    break;
                case LONGLONG:
                case TIMESTAMP:
                    builder.withLongField(field.getName(), (Long) field.getValue());
                    break;
                case SHORT_STRING:
                    builder.withShortStringField(field.getName(), (String) field.getValue());
                    break;
                case LONG_STRING:
                    builder.withLongStringField(field.getName(), (String) field.getValue());
                    break;
                case FIELD_TABLE:
                    builder.withTableField(field.getName(), (Map<String, Object>) field.getValue());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported field type: " + field.getType());
            }
        }
        
        return builder.build();
    }
    
    /**
     * Creates a heartbeat frame.
     * 
     * @param channel The channel number (should be 0)
     * @return Byte array representing the heartbeat frame
     * @throws IOException If frame creation fails
     */
    public static byte[] createHeartbeatFrame(short channel) throws IOException {
        return FrameFactory.heartbeatFrame()
                .withChannel(channel)
                .build();
    }
    
    /**
     * Creates a content header frame.
     * 
     * @param channel The channel number
     * @param classId The class ID (typically Basic.class = 60)
     * @param bodySize The total size of the content body
     * @return Byte array representing the content header frame
     * @throws IOException If frame creation fails
     */
    public static byte[] createContentHeaderFrame(short channel, short classId, long bodySize) throws IOException {
        return FrameFactory.headerFrame()
                .withChannel(channel)
                .withClassId(classId)
                .withBodySize(bodySize)
                .build();
    }
    
    /**
     * Creates a content body frame.
     * 
     * @param channel The channel number
     * @param content The content data
     * @return Byte array representing the content body frame
     * @throws IOException If frame creation fails
     */
    public static byte[] createContentBodyFrame(short channel, byte[] content) throws IOException {
        return FrameFactory.bodyFrame()
                .withChannel(channel)
                .withContent(content)
                .build();
    }
}
