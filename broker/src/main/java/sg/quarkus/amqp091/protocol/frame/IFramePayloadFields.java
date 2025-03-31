package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;

import java.util.List;

public interface IFramePayloadFields {
    List<AMQPDataField> getDataFields();
}
