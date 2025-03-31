package sg.quarkus.amqp091.protocol.frame;

public interface IFramePayloadMethod {
    int METHOD_INDEX = 9;

    int getMethodId();
}
