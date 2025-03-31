package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

public class DiscoveryFrame extends AbstractFrame {
    public DiscoveryFrame(byte[] data) throws UnprocessableFrameException {
        super(data);
    }

    @Override
    protected void checkLength() throws UnprocessableFrameException {
        if (data.length != 7) {
            throw new UnprocessableFrameException("Discovery frame needs to be 7 bytes long");
        }
    }

    @Override
    protected void checkEndByte() throws UnprocessableFrameException {
        // empty because the Header Frame does not have an end byte
    }
}
