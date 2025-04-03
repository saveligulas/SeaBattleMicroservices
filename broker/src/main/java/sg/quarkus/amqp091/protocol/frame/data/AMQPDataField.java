package sg.quarkus.amqp091.protocol.frame.data;

import java.util.Objects;

public class AMQPDataField {
    private final String name;
    private final Object value;
    private final AMQPDataType type;

    public AMQPDataField(String name, Object value, AMQPDataType type) {
        this.name = name;
        this.value = value;
        this.type = type;
        validateValueType();
    }

    public AMQPDataField(String name, Object value, String domainName) {
        this(name, value, AMQPDataType.getTypeByDomainName(domainName));
    }

    public AMQPDataField(Object value, AMQPDataType type) {
        this("", value, type);
    }

    public AMQPDataField(AMQPDataType type) {
        this("", null, type);
    }

    private void validateValueType() {
        if (value == null) {
            if (type != AMQPDataType.VOID) {
                throw new IllegalArgumentException("Null value only allowed for VOID type");
            }
            return;
        }

        boolean isValid = switch (type) {
            case BIT -> value instanceof Boolean;
            case OCTET -> value instanceof Byte;
            case SHORT -> value instanceof Short;
            case INT -> value instanceof Integer;
            case LONG -> value instanceof Integer || value instanceof Long;
            case LONGLONG, TIMESTAMP -> value instanceof Long;
            case FLOAT -> value instanceof Float;
            case DOUBLE -> value instanceof Double;
            case DECIMAL -> value instanceof java.math.BigDecimal;
            case SHORT_STRING, LONG_STRING -> value instanceof String;
            case FIELD_TABLE -> value instanceof java.util.Map;
            case VOID -> value == null;
        };

        if (!isValid) {
            throw new IllegalArgumentException("Invalid value type for " + type + ": " + value.getClass());
        }
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public AMQPDataType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AMQPDataField that = (AMQPDataField) o;
        return Objects.equals(name, that.name) && 
               Objects.equals(value, that.value) && 
               type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value, type);
    }

    @Override
    public String toString() {
        return "AMQPDataField{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", type=" + type +
                '}';
    }
}
