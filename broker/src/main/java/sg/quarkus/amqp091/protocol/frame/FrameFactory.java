package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;
import sg.quarkus.amqp091.protocol.frame.data.AMQPDataType;
import sg.quarkus.amqp091.protocol.frame.data.AMQPString;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrameFactory {
    private static final byte METHOD_FRAME_TYPE = 1;
    private static final byte HEADER_FRAME_TYPE = 2;
    private static final byte BODY_FRAME_TYPE = 3;
    private static final byte HEARTBEAT_FRAME_TYPE = 8;

    public static IFrame parseFrame(byte[] data) throws UnprocessableFrameException {
        if (data.length < 7) {
            throw new UnprocessableFrameException("Frame too short to contain a valid header");
        }
        
        byte frameType = data[0];

        return switch (frameType) {
            case METHOD_FRAME_TYPE -> new MethodFrame(data);
            case HEADER_FRAME_TYPE -> new HeaderFrame(data);
            case BODY_FRAME_TYPE -> new BodyFrame(data);
            case HEARTBEAT_FRAME_TYPE -> new HeartbeatFrame(data);
            default -> throw new UnprocessableFrameException("Unknown frame type: " + frameType);
        };
    }
    
    public static IFrame parseProtocolHeader(byte[] data) throws UnprocessableFrameException {
        if (data.length != 8) {
            throw new UnprocessableFrameException("Invalid protocol header length");
        }
        
        for (int i = 0; i < 4; i++) {
            if (data[i] != Header.PROTOCOL_HEADER[i]) {
                throw new UnprocessableFrameException("Invalid protocol header identifier");
            }
        }
        
        return new DiscoveryFrame(data);
    }

    public static class MethodFrameBuilder {
        private short channel;
        private IFramePayloadClass payloadClass;
        private IFramePayloadMethod payloadMethod;
        private List<AMQPDataField> fields = new ArrayList<>();
        
        public MethodFrameBuilder withChannel(short channel) {
            this.channel = channel;
            return this;
        }
        
        public MethodFrameBuilder withClass(IFramePayloadClass payloadClass) {
            this.payloadClass = payloadClass;
            return this;
        }
        
        public MethodFrameBuilder withMethod(IFramePayloadMethod payloadMethod) {
            this.payloadMethod = payloadMethod;
            return this;
        }
        
        public MethodFrameBuilder withField(String name, Object value, AMQPDataType type) {
            this.fields.add(new AMQPDataField(name, value, type));
            return this;
        }
        
        public MethodFrameBuilder withBitField(String name, boolean value) {
            return withField(name, value, AMQPDataType.BIT);
        }
        
        public MethodFrameBuilder withOctetField(String name, byte value) {
            return withField(name, value, AMQPDataType.OCTET);
        }
        
        public MethodFrameBuilder withShortField(String name, short value) {
            return withField(name, value, AMQPDataType.SHORT);
        }
        
        public MethodFrameBuilder withIntField(String name, int value) {
            return withField(name, value, AMQPDataType.INT);
        }
        
        public MethodFrameBuilder withLongField(String name, long value) {
            return withField(name, value, AMQPDataType.LONG);
        }
        
        public MethodFrameBuilder withShortStringField(String name, String value) {
            return withField(name, value, AMQPDataType.SHORT_STRING);
        }
        
        public MethodFrameBuilder withLongStringField(String name, String value) {
            return withField(name, value, AMQPDataType.LONG_STRING);
        }
        
        public MethodFrameBuilder withTimestampField(String name, long timestamp) {
            return withField(name, timestamp, AMQPDataType.TIMESTAMP);
        }
        
        public MethodFrameBuilder withTableField(String name, Map<String, Object> table) {
            return withField(name, table, AMQPDataType.FIELD_TABLE);
        }
        
        public byte[] build() throws IOException {
            if (payloadClass == null || payloadMethod == null) {
                throw new IllegalStateException("Frame class and method must be specified");
            }
            
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            
            // Write class ID (2 bytes)
            bodyStream.write((payloadClass.getClassId() >> 8) & 0xFF);
            bodyStream.write(payloadClass.getClassId() & 0xFF);
            
            // Write method ID (2 bytes)
            bodyStream.write((payloadMethod.getMethodId() >> 8) & 0xFF);
            bodyStream.write(payloadMethod.getMethodId() & 0xFF);
            
            // Write fields
            writeBitFields(bodyStream);
            writeNonBitFields(bodyStream);
            
            byte[] body = bodyStream.toByteArray();
            
            // Create the full frame
            ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
            
            // Type (1 byte)
            frameStream.write(METHOD_FRAME_TYPE);
            
            // Channel (2 bytes)
            frameStream.write((channel >> 8) & 0xFF);
            frameStream.write(channel & 0xFF);
            
            // Size (4 bytes)
            frameStream.write((body.length >> 24) & 0xFF);
            frameStream.write((body.length >> 16) & 0xFF);
            frameStream.write((body.length >> 8) & 0xFF);
            frameStream.write(body.length & 0xFF);
            
            // Body
            frameStream.write(body);
            
            // End byte
            frameStream.write(IFrame.PAYLOAD_END_BYTE);
            
            return frameStream.toByteArray();
        }
        
        private void writeBitFields(ByteArrayOutputStream stream) throws IOException {
            List<AMQPDataField> bitFields = fields.stream()
                .filter(field -> field.getType() == AMQPDataType.BIT)
                .toList();
            
            if (bitFields.isEmpty()) {
                return;
            }
            
            byte bitAccumulator = 0;
            int bitPosition = 0;
            
            for (AMQPDataField field : bitFields) {
                boolean value = (Boolean) field.getValue();
                if (value) {
                    bitAccumulator |= (1 << bitPosition);
                }
                
                bitPosition++;
                
                if (bitPosition == 8) {
                    stream.write(bitAccumulator);
                    bitAccumulator = 0;
                    bitPosition = 0;
                }
            }
            
            if (bitPosition > 0) {
                stream.write(bitAccumulator);
            }
        }
        
        private void writeNonBitFields(ByteArrayOutputStream stream) throws IOException {
            List<AMQPDataField> nonBitFields = fields.stream()
                .filter(field -> field.getType() != AMQPDataType.BIT)
                .toList();
            
            for (AMQPDataField field : nonBitFields) {
                writeField(stream, field);
            }
        }
        
        private void writeField(ByteArrayOutputStream stream, AMQPDataField field) throws IOException {
            switch (field.getType()) {
                case OCTET:
                    stream.write((Byte) field.getValue());
                    break;
                case SHORT:
                    short shortValue = (Short) field.getValue();
                    stream.write((shortValue >> 8) & 0xFF);
                    stream.write(shortValue & 0xFF);
                    break;
                case INT:
                    int intValue = (Integer) field.getValue();
                    stream.write((intValue >> 24) & 0xFF);
                    stream.write((intValue >> 16) & 0xFF);
                    stream.write((intValue >> 8) & 0xFF);
                    stream.write(intValue & 0xFF);
                    break;
                case LONG:
                case TIMESTAMP:
                    long longValue = (Long) field.getValue();
                    stream.write((int) ((longValue >> 56) & 0xFF));
                    stream.write((int) ((longValue >> 48) & 0xFF));
                    stream.write((int) ((longValue >> 40) & 0xFF));
                    stream.write((int) ((longValue >> 32) & 0xFF));
                    stream.write((int) ((longValue >> 24) & 0xFF));
                    stream.write((int) ((longValue >> 16) & 0xFF));
                    stream.write((int) ((longValue >> 8) & 0xFF));
                    stream.write((int) (longValue & 0xFF));
                    break;
                case SHORT_STRING:
                    String shortStr = (String) field.getValue();
                    byte[] shortStrBytes = shortStr.getBytes(StandardCharsets.UTF_8);
                    if (shortStrBytes.length > 255) {
                        throw new IllegalArgumentException("Short string too long: " + shortStrBytes.length);
                    }
                    stream.write(shortStrBytes.length);
                    stream.write(shortStrBytes);
                    break;
                case LONG_STRING:
                    String longStr = (String) field.getValue();
                    byte[] longStrBytes = longStr.getBytes(StandardCharsets.UTF_8);
                    stream.write((longStrBytes.length >> 24) & 0xFF);
                    stream.write((longStrBytes.length >> 16) & 0xFF);
                    stream.write((longStrBytes.length >> 8) & 0xFF);
                    stream.write(longStrBytes.length & 0xFF);
                    stream.write(longStrBytes);
                    break;
                case FIELD_TABLE:
                    writeFieldTable(stream, (Map<String, Object>) field.getValue());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported field type: " + field.getType());
            }
        }
        
        private void writeFieldTable(ByteArrayOutputStream stream, Map<String, Object> table) throws IOException {
            if (table == null || table.isEmpty()) {
                // Write table size as 0
                stream.write(0);
                stream.write(0);
                stream.write(0);
                stream.write(0);
                return;
            }
            
            ByteArrayOutputStream tableStream = new ByteArrayOutputStream();
            
            for (Map.Entry<String, Object> entry : table.entrySet()) {
                String key = entry.getKey();
                byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
                
                // Write field name
                tableStream.write(keyBytes.length);
                tableStream.write(keyBytes);
                
                // Write field value
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    tableStream.write('S'); // Long string
                    byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                    tableStream.write((strBytes.length >> 24) & 0xFF);
                    tableStream.write((strBytes.length >> 16) & 0xFF);
                    tableStream.write((strBytes.length >> 8) & 0xFF);
                    tableStream.write(strBytes.length & 0xFF);
                    tableStream.write(strBytes);
                } else if (value instanceof Integer) {
                    tableStream.write('I'); // Integer
                    int intValue = (Integer) value;
                    tableStream.write((intValue >> 24) & 0xFF);
                    tableStream.write((intValue >> 16) & 0xFF);
                    tableStream.write((intValue >> 8) & 0xFF);
                    tableStream.write(intValue & 0xFF);
                } else if (value instanceof Long) {
                    tableStream.write('L'); // Long
                    long longValue = (Long) value;
                    tableStream.write((int) ((longValue >> 56) & 0xFF));
                    tableStream.write((int) ((longValue >> 48) & 0xFF));
                    tableStream.write((int) ((longValue >> 40) & 0xFF));
                    tableStream.write((int) ((longValue >> 32) & 0xFF));
                    tableStream.write((int) ((longValue >> 24) & 0xFF));
                    tableStream.write((int) ((longValue >> 16) & 0xFF));
                    tableStream.write((int) ((longValue >> 8) & 0xFF));
                    tableStream.write((int) (longValue & 0xFF));
                } else if (value instanceof Boolean) {
                    tableStream.write('t'); // Boolean
                    tableStream.write((Boolean) value ? 1 : 0);
                } else if (value instanceof Map) {
                    tableStream.write('F'); // Nested table
                    writeFieldTable(tableStream, (Map<String, Object>) value);
                } else {
                    throw new IllegalArgumentException("Unsupported table value type: " + value.getClass());
                }
            }
            
            byte[] tableBytes = tableStream.toByteArray();
            
            // Write table size
            stream.write((tableBytes.length >> 24) & 0xFF);
            stream.write((tableBytes.length >> 16) & 0xFF);
            stream.write((tableBytes.length >> 8) & 0xFF);
            stream.write(tableBytes.length & 0xFF);
            
            // Write table content
            stream.write(tableBytes);
        }
    }
    
    public static class HeaderFrameBuilder {
        private short channel;
        private short classId;
        private long bodySize;
        private Map<String, Object> properties = new HashMap<>();
        
        public HeaderFrameBuilder withChannel(short channel) {
            this.channel = channel;
            return this;
        }
        
        public HeaderFrameBuilder withClassId(short classId) {
            this.classId = classId;
            return this;
        }
        
        public HeaderFrameBuilder withBodySize(long bodySize) {
            this.bodySize = bodySize;
            return this;
        }
        
        public HeaderFrameBuilder withProperty(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public byte[] build() throws IOException {
            ByteArrayOutputStream bodyStream = new ByteArrayOutputStream();
            
            // Class ID (2 bytes)
            bodyStream.write((classId >> 8) & 0xFF);
            bodyStream.write(classId & 0xFF);
            
            // Weight (2 bytes, always 0)
            bodyStream.write(0);
            bodyStream.write(0);
            
            // Body size (8 bytes)
            bodyStream.write((int) ((bodySize >> 56) & 0xFF));
            bodyStream.write((int) ((bodySize >> 48) & 0xFF));
            bodyStream.write((int) ((bodySize >> 40) & 0xFF));
            bodyStream.write((int) ((bodySize >> 32) & 0xFF));
            bodyStream.write((int) ((bodySize >> 24) & 0xFF));
            bodyStream.write((int) ((bodySize >> 16) & 0xFF));
            bodyStream.write((int) ((bodySize >> 8) & 0xFF));
            bodyStream.write((int) (bodySize & 0xFF));
            
            // Property flags and values
            // This is a simplified implementation - real AMQP would need to handle proper property flags
            ByteArrayOutputStream propsStream = new ByteArrayOutputStream();
            int propFlags = 0;
            
            // For simplicity, we'll just write a dummy property list
            // In a complete implementation, you would properly set property flags
            
            // Write property flags (2 bytes)
            bodyStream.write((propFlags >> 8) & 0xFF);
            bodyStream.write(propFlags & 0xFF);
            
            // Write property values
            bodyStream.write(propsStream.toByteArray());
            
            byte[] body = bodyStream.toByteArray();
            
            ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
            
            // Type (1 byte)
            frameStream.write(HEADER_FRAME_TYPE);
            
            // Channel (2 bytes)
            frameStream.write((channel >> 8) & 0xFF);
            frameStream.write(channel & 0xFF);
            
            // Size (4 bytes)
            frameStream.write((body.length >> 24) & 0xFF);
            frameStream.write((body.length >> 16) & 0xFF);
            frameStream.write((body.length >> 8) & 0xFF);
            frameStream.write(body.length & 0xFF);
            
            // Body
            frameStream.write(body);
            
            // End byte
            frameStream.write(IFrame.PAYLOAD_END_BYTE);
            
            return frameStream.toByteArray();
        }
    }
    
    public static class BodyFrameBuilder {
        private short channel;
        private byte[] content;
        
        public BodyFrameBuilder withChannel(short channel) {
            this.channel = channel;
            return this;
        }
        
        public BodyFrameBuilder withContent(byte[] content) {
            this.content = content;
            return this;
        }
        
        public byte[] build() throws IOException {
            ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
            
            // Type (1 byte)
            frameStream.write(BODY_FRAME_TYPE);
            
            // Channel (2 bytes)
            frameStream.write((channel >> 8) & 0xFF);
            frameStream.write(channel & 0xFF);
            
            // Size (4 bytes)
            frameStream.write((content.length >> 24) & 0xFF);
            frameStream.write((content.length >> 16) & 0xFF);
            frameStream.write((content.length >> 8) & 0xFF);
            frameStream.write(content.length & 0xFF);
            
            // Content
            frameStream.write(content);
            
            // End byte
            frameStream.write(IFrame.PAYLOAD_END_BYTE);
            
            return frameStream.toByteArray();
        }
    }
    
    public static class HeartbeatFrameBuilder {
        private short channel;
        
        public HeartbeatFrameBuilder withChannel(short channel) {
            this.channel = channel;
            return this;
        }
        
        public byte[] build() throws IOException {
            ByteArrayOutputStream frameStream = new ByteArrayOutputStream();
            
            // Type (1 byte)
            frameStream.write(HEARTBEAT_FRAME_TYPE);
            
            // Channel (2 bytes)
            frameStream.write((channel >> 8) & 0xFF);
            frameStream.write(channel & 0xFF);
            
            // Size (4 bytes) - always 0 for heartbeat
            frameStream.write(0);
            frameStream.write(0);
            frameStream.write(0);
            frameStream.write(0);
            
            // End byte
            frameStream.write(IFrame.PAYLOAD_END_BYTE);
            
            return frameStream.toByteArray();
        }
    }
    
    // Static factory methods for convenience
    public static MethodFrameBuilder methodFrame() {
        return new MethodFrameBuilder();
    }
    
    public static HeaderFrameBuilder headerFrame() {
        return new HeaderFrameBuilder();
    }
    
    public static BodyFrameBuilder bodyFrame() {
        return new BodyFrameBuilder();
    }
    
    public static HeartbeatFrameBuilder heartbeatFrame() {
        return new HeartbeatFrameBuilder();
    }
}
