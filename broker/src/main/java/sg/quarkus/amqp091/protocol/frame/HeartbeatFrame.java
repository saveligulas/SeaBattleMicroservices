package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

public class HeartbeatFrame extends AbstractFrame {
    public HeartbeatFrame(byte[] data) throws UnprocessableFrameException {
        super(data);
    }
}
