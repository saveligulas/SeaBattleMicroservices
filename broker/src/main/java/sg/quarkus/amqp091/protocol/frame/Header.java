package sg.quarkus.amqp091.protocol.frame;

public class Header {
    public static final byte[] PROTOCOL_HEADER = new byte[] { 0x41, 0x4D, 0x51, 0x50, 0x00, 0, 9, 1 };

    private final byte[] data = new byte[7];

    public Header(byte[] data) {
        if (data.length != this.data.length) {
            throw new IllegalArgumentException("Payload length does not match header length");
        }
        System.arraycopy(data, 0, this.data, 0, data.length);
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


}
