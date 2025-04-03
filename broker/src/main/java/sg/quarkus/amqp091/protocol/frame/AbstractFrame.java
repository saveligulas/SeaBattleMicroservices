package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

public abstract class AbstractFrame implements IFrame {
    protected final byte[] data;

    public AbstractFrame(byte[] data) throws UnprocessableFrameException {
        this.data = data;
        checkLength();
        checkEndByte();
    }

    @Override
    public int getTotalSize() {
        return data.length;
    }

    @Override
    public int getTotalPayloadSize() {
        return data.length - Header.HEADER_LENGTH;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    public byte getMethodByte() {
        return this.data[0];
    }

    public short getChannelShort() {
        return (short) ((this.data[1] & 0xff) << 8 | (this.data[2] & 0xff));
    }

    public int getSizeLong() {
        return (this.data[3] & 0xff) << 24 | (this.data[4] & 0xff) << 16 | (this.data[5] & 0xff) << 8 | (this.data[6] & 0xff);
    }

    protected void checkLength() throws UnprocessableFrameException {
        if (data.length < Header.HEADER_LENGTH) {
            throw new UnprocessableFrameException("Frame is too short");
        }
    }

    //TODO: Override this in the connection discovery mechanism or Heartbeat
    protected void checkEndByte() throws UnprocessableFrameException {
        if (data[data.length - 1] != IFrame.PAYLOAD_END_BYTE) {
            throw new UnprocessableFrameException("Frame does not have the correct end Byte: " + this);
        }
    }
}
