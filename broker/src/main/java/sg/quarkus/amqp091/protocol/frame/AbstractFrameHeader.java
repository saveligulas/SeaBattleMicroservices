package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

public class AbstractFrameHeader extends AbstractFrame {
    public AbstractFrameHeader(byte[] data) throws UnprocessableFrameException {
        super(data);
    }

    @Override
    protected void checkEndByte() throws UnprocessableFrameException {
        if (data.length < 7) {

        }
    }
}
