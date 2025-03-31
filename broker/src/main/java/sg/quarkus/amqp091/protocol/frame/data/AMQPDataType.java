package sg.quarkus.amqp091.protocol.frame.data;

//TODO: add length
public enum AMQPDataType {
    BIT,
    OCTET,
    SHORT,
    INT,
    LONG,
    SHORT_STRING,
    LONG_STRING,
    TIMESTAMP,
    FIELD_TABLE;
}
