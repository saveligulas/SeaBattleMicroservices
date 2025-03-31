package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

public class BodyFrame extends AbstractFrame {

    public BodyFrame(byte[] data) throws UnprocessableFrameException {
        super(data);
    }
}
