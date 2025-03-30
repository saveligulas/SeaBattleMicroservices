package sg.quarkus.amqp091.protocol.frame;

public interface IAMQPPayloadHeader {
    int getClassId();
    int getMethodId();
}
