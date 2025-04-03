package sg.quarkus.amqp091.protocol.frame.data;

public enum AMQPDataType {
    BIT('t', -1), // Special case - bits are packed
    OCTET('B', 1),
    SHORT('u', 2),
    INT('i', 4),
    LONG('I', 4),
    LONGLONG('l', 8),
    FLOAT('f', 4),
    DOUBLE('d', 8),
    DECIMAL('D', 5), // 1 byte scale + 4 byte value
    SHORT_STRING('s', -1), // Variable length up to 255
    LONG_STRING('S', -1), // Variable length
    FIELD_TABLE('F', -1), // Variable length
    TIMESTAMP('T', 8),
    VOID('V', 0);

    private final char typeTag;
    private final int fixedSize; // -1 for variable size

    AMQPDataType(char typeTag, int fixedSize) {
        this.typeTag = typeTag;
        this.fixedSize = fixedSize;
    }

    public char getTypeTag() {
        return typeTag;
    }

    public int getFixedSize() {
        return fixedSize;
    }

    public boolean isFixedSize() {
        return fixedSize >= 0;
    }

    public static AMQPDataType fromTypeTag(char tag) {
        for (AMQPDataType type : values()) {
            if (type.getTypeTag() == tag) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown AMQP data type tag: " + tag);
    }

    // Domain-specific names mapped to their underlying type
    public static AMQPDataType getTypeByDomainName(String domainName) {
        return switch (domainName.toLowerCase()) {
            case "exchange-name", "queue-name", "consumer-tag", "path", "reply-text" -> SHORT_STRING;
            case "delivery-tag" -> LONGLONG;
            case "message-count", "reply-code" -> LONG;
            case "no-wait", "redelivered" -> BIT;
            case "class-id", "method-id" -> SHORT;
            case "peer-properties" -> FIELD_TABLE;
            default -> throw new IllegalArgumentException("Unknown domain name: " + domainName);
        };
    }
}
