package sg.quarkus.amqp091.protocol.frame.data;

public class AMQPDataField {
    private final AMQPDataType type;
    private byte[] value;

    public AMQPDataField(AMQPDataType type) {
        this(type, null);
    }

    public AMQPDataField(AMQPDataType type, byte[] value) {
        this.type = type;
        this.value = value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
