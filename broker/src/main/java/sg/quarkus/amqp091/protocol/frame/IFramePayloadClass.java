package sg.quarkus.amqp091.protocol.frame;

public interface IFramePayloadClass {
    int CLASS_INDEX = 7;

    int getClassId();
}
