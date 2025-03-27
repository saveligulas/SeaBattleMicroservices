package sg.quarkus.amqp091.protocol.frame;

public class Payload {
    private byte[] data;

    public Payload(byte[] data) {
        this.data = data;
    }


}
