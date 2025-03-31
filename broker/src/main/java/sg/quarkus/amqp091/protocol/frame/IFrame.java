package sg.quarkus.amqp091.protocol.frame;

public interface IFrame {
    byte PAYLOAD_END_BYTE = (byte) 0xCE;

    int getTotalSize();
    int getTotalPayloadSize();
    default int getPayloadSize() {
        return getTotalPayloadSize() - 1; //subtract the ending byte from the total payload size to get the payload size without the ending byte
    } //returns the payload size without the ending byte
    byte[] getData();
}
