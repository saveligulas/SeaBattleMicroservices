package sg.quarkus.amqp091.protocol.frame.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AMQPDataParser {
    
    // Method to serialize a data field to bytes
    public static byte[] serialize(AMQPDataField field) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        serialize(field, output);
        return output.toByteArray();
    }
    
    // Serialize a list of data fields, handling bit packing
    public static byte[] serializeFields(List<AMQPDataField> fields) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        
        // Group bit fields together for packing
        List<AMQPDataField> bitFields = new ArrayList<>();
        
        for (AMQPDataField field : fields) {
            if (field.getType() == AMQPDataType.BIT) {
                bitFields.add(field);
            } else {
                // If we have accumulated bit fields, write them now
                if (!bitFields.isEmpty()) {
                    serializeBitFields(bitFields, output);
                    bitFields.clear();
                }
                serialize(field, output);
            }
        }
        
        // Write any remaining bit fields
        if (!bitFields.isEmpty()) {
            serializeBitFields(bitFields, output);
        }
        
        return output.toByteArray();
    }
    
    // Helper method to serialize a single field to the output stream
    private static void serialize(AMQPDataField field, ByteArrayOutputStream output) throws IOException {
        AMQPDataType type = field.getType();
        Object value = field.getValue();
        
        switch (type) {
            case BIT -> {
                // Individual bits are handled in serializeBitFields
                throw new IllegalArgumentException("BIT fields should be handled by serializeBitFields");
            }
            case OCTET -> output.write((byte) value);
            case SHORT -> {
                short shortVal = (short) value;
                output.write((shortVal >> 8) & 0xFF);
                output.write(shortVal & 0xFF);
            }
            case INT, LONG -> {
                int intVal = (int) value;
                output.write((intVal >> 24) & 0xFF);
                output.write((intVal >> 16) & 0xFF);
                output.write((intVal >> 8) & 0xFF);
                output.write(intVal & 0xFF);
            }
            case LONGLONG, TIMESTAMP -> {
                long longVal = (long) value;
                output.write((int) ((longVal >> 56) & 0xFF));
                output.write((int) ((longVal >> 48) & 0xFF));
                output.write((int) ((longVal >> 40) & 0xFF));
                output.write((int) ((longVal >> 32) & 0xFF));
                output.write((int) ((longVal >> 24) & 0xFF));
                output.write((int) ((longVal >> 16) & 0xFF));
                output.write((int) ((longVal >> 8) & 0xFF));
                output.write((int) (longVal & 0xFF));
            }
            case FLOAT -> {
                int intBits = Float.floatToIntBits((float) value);
                output.write((intBits >> 24) & 0xFF);
                output.write((intBits >> 16) & 0xFF);
                output.write((intBits >> 8) & 0xFF);
                output.write(intBits & 0xFF);
            }
            case DOUBLE -> {
                long longBits = Double.doubleToLongBits((double) value);
                output.write((int) ((longBits >> 56) & 0xFF));
                output.write((int) ((longBits >> 48) & 0xFF));
                output.write((int) ((longBits >> 40) & 0xFF));
                output.write((int) ((longBits >> 32) & 0xFF));
                output.write((int) ((longBits >> 24) & 0xFF));
                output.write((int) ((longBits >> 16) & 0xFF));
                output.write((int) ((longBits >> 8) & 0xFF));
                output.write((int) (longBits & 0xFF));
            }
            case DECIMAL -> {
                BigDecimal decimal = (BigDecimal) value;
                int scale = decimal.scale();
                int unscaledValue = decimal.unscaledValue().intValue();
                output.write(scale);
                output.write((unscaledValue >> 24) & 0xFF);
                output.write((unscaledValue >> 16) & 0xFF);
                output.write((unscaledValue >> 8) & 0xFF);
                output.write(unscaledValue & 0xFF);
            }
            case SHORT_STRING -> {
                byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                if (strBytes.length > 255) {
                    throw new IllegalArgumentException("Short string too long: " + strBytes.length);
                }
                output.write(strBytes.length);
                output.write(strBytes);
            }
            case LONG_STRING -> {
                byte[] strBytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                output.write((strBytes.length >> 24) & 0xFF);
                output.write((strBytes.length >> 16) & 0xFF);
                output.write((strBytes.length >> 8) & 0xFF);
                output.write(strBytes.length & 0xFF);
                output.write(strBytes);
            }
            case FIELD_TABLE -> {
                ByteArrayOutputStream tableData = new ByteArrayOutputStream();
                serializeFieldTable((Map<String, Object>) value, tableData);
                byte[] tableBytes = tableData.toByteArray();
                
                // Write table size
                output.write((tableBytes.length >> 24) & 0xFF);
                output.write((tableBytes.length >> 16) & 0xFF);
                output.write((tableBytes.length >> 8) & 0xFF);
                output.write(tableBytes.length & 0xFF);
                
                // Write table content
                output.write(tableBytes);
            }
            case VOID -> {
                // Nothing to write for void
            }
        }
    }
    
    // Helper method to serialize multiple bit fields, packing them into bytes
    private static void serializeBitFields(List<AMQPDataField> bitFields, ByteArrayOutputStream output) throws IOException {
        if (bitFields.isEmpty()) {
            return;
        }
        
        byte currentByte = 0;
        int bitPosition = 0;
        
        for (AMQPDataField field : bitFields) {
            if (field.getType() != AMQPDataType.BIT) {
                throw new IllegalArgumentException("Expected BIT field, got " + field.getType());
            }
            
            if ((boolean) field.getValue()) {
                currentByte |= (1 << bitPosition);
            }
            
            bitPosition++;
            
            if (bitPosition == 8) {
                output.write(currentByte);
                currentByte = 0;
                bitPosition = 0;
            }
        }
        
        // Write the last byte if we have any bits set
        if (bitPosition > 0) {
            output.write(currentByte);
        }
    }
    
    // Helper method to serialize a field table
    private static void serializeFieldTable(Map<String, Object> table, ByteArrayOutputStream output) throws IOException {
        if (table == null || table.isEmpty()) {
            return;
        }
        
        for (Map.Entry<String, Object> entry : table.entrySet()) {
            // Write field name (short string)
            String key = entry.getKey();
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length > 255) {
                throw new IllegalArgumentException("Field name too long: " + keyBytes.length);
            }
            output.write(keyBytes.length);
            output.write(keyBytes);
            
            Object value = entry.getValue();
            
            // Write field value type and value
            if (value == null) {
                output.write('V'); // Void
            } else if (value instanceof Boolean) {
                output.write('t'); // Boolean
                output.write((boolean) value ? 1 : 0);
            } else if (value instanceof Byte) {
                output.write('B'); // Octet
                output.write((byte) value);
            } else if (value instanceof Short) {
                output.write('u'); // Short
                short shortVal = (short) value;
                output.write((shortVal >> 8) & 0xFF);
                output.write(shortVal & 0xFF);
            } else if (value instanceof Integer) {
                output.write('I'); // Long (32-bit integer)
                int intVal = (int) value;
                output.write((intVal >> 24) & 0xFF);
                output.write((intVal >> 16) & 0xFF);
                output.write((intVal >> 8) & 0xFF);
                output.write(intVal & 0xFF);
            } else if (value instanceof Long) {
                output.write('l'); // Long long
                long longVal = (long) value;
                output.write((int) ((longVal >> 56) & 0xFF));
                output.write((int) ((longVal >> 48) & 0xFF));
                output.write((int) ((longVal >> 40) & 0xFF));
                output.write((int) ((longVal >> 32) & 0xFF));
                output.write((int) ((longVal >> 24) & 0xFF));
                output.write((int) ((longVal >> 16) & 0xFF));
                output.write((int) ((longVal >> 8) & 0xFF));
                output.write((int) (longVal & 0xFF));
            } else if (value instanceof Float) {
                output.write('f'); // Float
                int intBits = Float.floatToIntBits((float) value);
                output.write((intBits >> 24) & 0xFF);
                output.write((intBits >> 16) & 0xFF);
                output.write((intBits >> 8) & 0xFF);
                output.write(intBits & 0xFF);
            } else if (value instanceof Double) {
                output.write('d'); // Double
                long longBits = Double.doubleToLongBits((double) value);
                output.write((int) ((longBits >> 56) & 0xFF));
                output.write((int) ((longBits >> 48) & 0xFF));
                output.write((int) ((longBits >> 40) & 0xFF));
                output.write((int) ((longBits >> 32) & 0xFF));
                output.write((int) ((longBits >> 24) & 0xFF));
                output.write((int) ((longBits >> 16) & 0xFF));
                output.write((int) ((longBits >> 8) & 0xFF));
                output.write((int) (longBits & 0xFF));
            } else if (value instanceof BigDecimal) {
                output.write('D'); // Decimal
                BigDecimal decimal = (BigDecimal) value;
                int scale = decimal.scale();
                int unscaledValue = decimal.unscaledValue().intValue();
                output.write(scale);
                output.write((unscaledValue >> 24) & 0xFF);
                output.write((unscaledValue >> 16) & 0xFF);
                output.write((unscaledValue >> 8) & 0xFF);
                output.write(unscaledValue & 0xFF);
            } else if (value instanceof String) {
                String str = (String) value;
                byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
                output.write('S'); // Long string
                output.write((strBytes.length >> 24) & 0xFF);
                output.write((strBytes.length >> 16) & 0xFF);
                output.write((strBytes.length >> 8) & 0xFF);
                output.write(strBytes.length & 0xFF);
                output.write(strBytes);
            } else if (value instanceof Map) {
                output.write('F'); // Field table
                ByteArrayOutputStream nestedTable = new ByteArrayOutputStream();
                serializeFieldTable((Map<String, Object>) value, nestedTable);
                byte[] tableBytes = nestedTable.toByteArray();
                output.write((tableBytes.length >> 24) & 0xFF);
                output.write((tableBytes.length >> 16) & 0xFF);
                output.write((tableBytes.length >> 8) & 0xFF);
                output.write(tableBytes.length & 0xFF);
                output.write(tableBytes);
            } else if (value instanceof byte[]) {
                output.write('x'); // Binary
                byte[] binData = (byte[]) value;
                output.write((binData.length >> 24) & 0xFF);
                output.write((binData.length >> 16) & 0xFF);
                output.write((binData.length >> 8) & 0xFF);
                output.write(binData.length & 0xFF);
                output.write(binData);
            } else {
                throw new IllegalArgumentException("Unsupported field table value type: " + value.getClass());
            }
        }
    }
    
    // Method to deserialize a data field from bytes
    public static AMQPDataField deserialize(String name, AMQPDataType type, ByteBuffer buffer) {
        Object value = deserializeValue(type, buffer);
        return new AMQPDataField(name, value, type);
    }
    
    // Deserialize multiple fields from a byte array
    public static List<AMQPDataField> deserializeFields(List<FieldDefinition> fieldDefinitions, byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        List<AMQPDataField> fields = new ArrayList<>();
        
        // Group bit fields for unpacking
        List<FieldDefinition> bitFields = new ArrayList<>();
        
        for (FieldDefinition def : fieldDefinitions) {
            if (def.type() == AMQPDataType.BIT) {
                bitFields.add(def);
            } else {
                // If we have accumulated bit fields, read them now
                if (!bitFields.isEmpty()) {
                    fields.addAll(deserializeBitFields(bitFields, buffer));
                    bitFields.clear();
                }
                
                fields.add(deserialize(def.name(), def.type(), buffer));
            }
        }
        
        // Read any remaining bit fields
        if (!bitFields.isEmpty()) {
            fields.addAll(deserializeBitFields(bitFields, buffer));
        }
        
        return fields;
    }
    
    // Helper method to deserialize a single value
    private static Object deserializeValue(AMQPDataType type, ByteBuffer buffer) {
        return switch (type) {
            case BIT -> buffer.get() != 0;
            case OCTET -> buffer.get();
            case SHORT -> buffer.getShort();
            case INT, LONG -> buffer.getInt();
            case LONGLONG, TIMESTAMP -> buffer.getLong();
            case FLOAT -> buffer.getFloat();
            case DOUBLE -> buffer.getDouble();
            case DECIMAL -> {
                byte scale = buffer.get();
                int unscaledValue = buffer.getInt();
                yield new BigDecimal(unscaledValue).movePointLeft(scale);
            }
            case SHORT_STRING -> {
                int length = buffer.get() & 0xFF;
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                yield new String(bytes, StandardCharsets.UTF_8);
            }
            case LONG_STRING -> {
                int length = buffer.getInt();
                byte[] bytes = new byte[length];
                buffer.get(bytes);
                yield new String(bytes, StandardCharsets.UTF_8);
            }
            case FIELD_TABLE -> deserializeFieldTable(buffer);
            case VOID -> null;
        };
    }
    
    // Helper method to deserialize multiple bit fields
    private static List<AMQPDataField> deserializeBitFields(List<FieldDefinition> bitFields, ByteBuffer buffer) {
        List<AMQPDataField> fields = new ArrayList<>();
        
        if (bitFields.isEmpty()) {
            return fields;
        }
        
        int bitPosition = 0;
        byte currentByte = 0;
        
        for (FieldDefinition def : bitFields) {
            if (def.type() != AMQPDataType.BIT) {
                throw new IllegalArgumentException("Expected BIT field, got " + def.type());
            }
            
            if (bitPosition == 0) {
                currentByte = buffer.get();
            }
            
            boolean value = (currentByte & (1 << bitPosition)) != 0;
            fields.add(new AMQPDataField(def.name(), value, AMQPDataType.BIT));
            
            bitPosition = (bitPosition + 1) % 8;
        }
        
        return fields;
    }
    
    // Helper method to deserialize a field table
    private static Map<String, Object> deserializeFieldTable(ByteBuffer buffer) {
        int tableSize = buffer.getInt();
        if (tableSize == 0) {
            return new HashMap<>();
        }
        
        int endPosition = buffer.position() + tableSize;
        Map<String, Object> table = new HashMap<>();
        
        while (buffer.position() < endPosition) {
            // Read field name (short string)
            int nameLength = buffer.get() & 0xFF;
            byte[] nameBytes = new byte[nameLength];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            
            // Read field value type
            char typeTag = (char) buffer.get();
            
            // Read field value
            Object value = switch (typeTag) {
                case 't' -> buffer.get() != 0; // Boolean
                case 'b' -> buffer.get(); // Signed 8-bit
                case 'B' -> buffer.get() & 0xFF; // Unsigned 8-bit
                case 'U' -> (short) (buffer.getShort() & 0xFFFF); // Unsigned 16-bit
                case 'u' -> buffer.getShort(); // Signed 16-bit
                case 'I' -> buffer.getInt(); // Signed 32-bit
                case 'i' -> buffer.getInt(); // Signed 32-bit
                case 'L' -> buffer.getLong() & 0xFFFFFFFFFFFFFFFFL; // Unsigned 64-bit
                case 'l' -> buffer.getLong(); // Signed 64-bit
                case 'f' -> buffer.getFloat(); // 32-bit float
                case 'd' -> buffer.getDouble(); // 64-bit float
                case 'D' -> { // Decimal
                    byte scale = buffer.get();
                    int unscaledValue = buffer.getInt();
                    yield new BigDecimal(unscaledValue).movePointLeft(scale);
                }
                case 's' -> { // Short string
                    int length = buffer.get() & 0xFF;
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    yield new String(bytes, StandardCharsets.UTF_8);
                }
                case 'S' -> { // Long string
                    int length = buffer.getInt();
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    yield new String(bytes, StandardCharsets.UTF_8);
                }
                case 'A' -> { // Array
                    int length = buffer.getInt();
                    // Skip arrays for now - more complex implementation needed
                    buffer.position(buffer.position() + length);
                    yield null;
                }
                case 'T' -> buffer.getLong(); // Timestamp (64-bit)
                case 'F' -> deserializeFieldTable(buffer); // Nested field table
                case 'V' -> null; // Void
                case 'x' -> { // Binary
                    int length = buffer.getInt();
                    byte[] bytes = new byte[length];
                    buffer.get(bytes);
                    yield bytes;
                }
                default -> {
                    throw new IllegalArgumentException("Unknown field table value type tag: " + typeTag);
                }
            };
            
            table.put(name, value);
        }
        
        return table;
    }
    
    // Record for field definitions, used when deserializing multiple fields
    public record FieldDefinition(String name, AMQPDataType type) {
        public static FieldDefinition of(String name, AMQPDataType type) {
            return new FieldDefinition(name, type);
        }
        
        public static FieldDefinition ofDomain(String name, String domainName) {
            return new FieldDefinition(name, AMQPDataType.getTypeByDomainName(domainName));
        }
    }
}
