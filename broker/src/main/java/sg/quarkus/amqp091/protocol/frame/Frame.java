package sg.quarkus.amqp091.protocol.frame;

import java.util.Arrays;

public class Frame {
    public static final byte FRAME_END = (byte) 0xCE;

    private final byte[] data;
    private final Header header;

    public Frame(byte[] data) {
        if (data.length < 7) {
            throw new IllegalArgumentException("Frame must be minimum 7 bytes long");
        }

        this.data = data;
        this.header = new Header(Arrays.copyOfRange(data, 0, 7));
    }


}
