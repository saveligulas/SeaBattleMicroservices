package sg.quarkus.amqp091.protocol.frame.data;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class PropertyFlagParser {
    //TODO: decide whether to change param to payload
    public static List<AMQPDataField> convertToEmptyAMQPDataFields(byte[] bitmask) {
        List<AMQPDataField> fields = new ArrayList<>();

        BitSet bitSet = convertToBitSet(bitmask);

        if (bitSet.get(0)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // content-type
        if (bitSet.get(1)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // content-encoding
        if (bitSet.get(2)) fields.add(new AMQPDataField(AMQPDataType.FIELD_TABLE)); // headers
        if (bitSet.get(3)) fields.add(new AMQPDataField(AMQPDataType.OCTET)); // delivery-mode
        if (bitSet.get(4)) fields.add(new AMQPDataField(AMQPDataType.OCTET)); // priority
        if (bitSet.get(5)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // correlation-id
        if (bitSet.get(6)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // reply-to
        if (bitSet.get(7)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // expiration
        if (bitSet.get(8)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // message-id
        if (bitSet.get(9)) fields.add(new AMQPDataField(AMQPDataType.TIMESTAMP)); // timestamp
        if (bitSet.get(10)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // type
        if (bitSet.get(11)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // user-id
        if (bitSet.get(12)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // app-id
        if (bitSet.get(13)) fields.add(new AMQPDataField(AMQPDataType.SHORT_STRING)); // reserved

        return fields;
    }

    private static BitSet convertToBitSet(byte[] bitmask) {
        BitSet bitSet = new BitSet();

        int bitPosition = 0;

        for (int i = 0; i < bitmask.length; i += 2) {
            // Each segment is 2 bytes (short)
            if (i + 1 >= bitmask.length) {
                break; // Malformed bitmask
            }

            // Convert two bytes to a short (big-endian)
            int shortValue = ((bitmask[i] & 0xFF) << 8) | (bitmask[i + 1] & 0xFF);

            // Process the 15 usable bits (skip the continuation bit)
            for (int j = 0; j < 15; j++) {
                if ((shortValue & (1 << (15 - j))) != 0) {
                    bitSet.set(bitPosition);
                }
                bitPosition++;
            }

            // Check continuation bit
            boolean hasMore = (shortValue & 1) == 1;
            if (!hasMore) {
                break;
            }
        }

        return bitSet;
    }

    public static String getPropertyNameByIndex(int index) {
        return switch (index) {
            case 0 -> "content-type";
            case 1 -> "content-encoding";
            case 2 -> "headers";
            case 3 -> "delivery-mode";
            case 4 -> "priority";
            case 5 -> "correlation-id";
            case 6 -> "reply-to";
            case 7 -> "expiration";
            case 8 -> "message-id";
            case 9 -> "timestamp";
            case 10 -> "type";
            case 11 -> "user-id";
            case 12 -> "app-id";
            case 13 -> "reserved";
            default -> "unknown";
        };
    }
}

