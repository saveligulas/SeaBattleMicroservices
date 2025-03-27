package sg.quarkus.amqp091.protocol.frame.data;

import sg.quarkus.amqp091.protocol.frame.NumberUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class AMQPString {
    protected String string;

    private static void validateFormat(int length, byte[] data) {
        if (length != data.length) {
            throw new IllegalArgumentException("Invalid AMQP string length");
        }
    }

    public static class Short extends AMQPString {
        public Short(byte[] data) {
            AMQPString.validateFormat(data[0], data);
            byte[] truncatedData = new byte[data.length - 1];
            System.arraycopy(data, 1, truncatedData, 0, truncatedData.length);
            this.string = Arrays.toString(truncatedData);
        }
    }

    public static class Long extends AMQPString {
        private static int getLength(byte[] data) {
            return NumberUtils.toInt(data, 0);
        }

        public Long(byte[] data) {
            AMQPString.validateFormat(getLength(data), data);
            byte[] truncatedData = new byte[data.length - 4];
            System.arraycopy(data, 4, truncatedData, 0, truncatedData.length);
            this.string = Arrays.toString(truncatedData);
        }
    }

    public AMQPString() {
    }

    public AMQPString(byte[] truncatedData) {
        this.string = new String(truncatedData, StandardCharsets.UTF_8);
    }
}
