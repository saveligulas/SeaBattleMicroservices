package sg.quarkus.amqp091.protocol.frame.error;

public class UnprocessableFrameException extends Exception {
    public UnprocessableFrameException(String message) {
        super(message);
    }
}
