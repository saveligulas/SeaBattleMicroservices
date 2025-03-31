package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.util.List;

public class MethodFrame extends AbstractFrame implements IFramePayloadClass, IFramePayloadMethod, IFramePayloadFields {

    public MethodFrame(byte[] data) throws UnprocessableFrameException {
        super(data);
    }

    @Override
    public int getClassId() {
        return NumberUtils.toShort(data[7], data[8]);
    }

    @Override
    public int getMethodId() {
        return NumberUtils.toShort(data[9], data[10]);
    }

    @Override
    public List<AMQPDataField> getDataFields() {
        return List.of();
    }
}
