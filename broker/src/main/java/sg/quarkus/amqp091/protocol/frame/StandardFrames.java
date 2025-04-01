package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;
import sg.quarkus.amqp091.protocol.frame.data.AMQPDataType;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Standard frame instances for AMQP 0-9-1 protocol.
 * Contains static singletons for common frame types with empty data fields.
 */
public class StandardFrames {
    // Protocol header (not a frame, but needed for connection)
    public static final byte[] PROTOCOL_HEADER = new byte[] { 'A', 'M', 'Q', 'P', 0, 0, 9, 1 };
    
    // Connection frames
    public static final byte[] CONNECTION_START;
    public static final byte[] CONNECTION_START_OK;
    public static final byte[] CONNECTION_TUNE;
    public static final byte[] CONNECTION_TUNE_OK;
    public static final byte[] CONNECTION_OPEN;
    public static final byte[] CONNECTION_OPEN_OK;
    public static final byte[] CONNECTION_CLOSE;
    public static final byte[] CONNECTION_CLOSE_OK;
    
    // Channel frames
    public static final byte[] CHANNEL_OPEN;
    public static final byte[] CHANNEL_OPEN_OK;
    public static final byte[] CHANNEL_CLOSE;
    public static final byte[] CHANNEL_CLOSE_OK;
    public static final byte[] CHANNEL_FLOW;
    public static final byte[] CHANNEL_FLOW_OK;
    
    // Exchange frames
    public static final byte[] EXCHANGE_DECLARE;
    public static final byte[] EXCHANGE_DECLARE_OK;
    public static final byte[] EXCHANGE_DELETE;
    public static final byte[] EXCHANGE_DELETE_OK;
    
    // Queue frames
    public static final byte[] QUEUE_DECLARE;
    public static final byte[] QUEUE_DECLARE_OK;
    public static final byte[] QUEUE_BIND;
    public static final byte[] QUEUE_BIND_OK;
    public static final byte[] QUEUE_UNBIND;
    public static final byte[] QUEUE_UNBIND_OK;
    public static final byte[] QUEUE_PURGE;
    public static final byte[] QUEUE_PURGE_OK;
    public static final byte[] QUEUE_DELETE;
    public static final byte[] QUEUE_DELETE_OK;
    
    // Basic frames
    public static final byte[] BASIC_QOS;
    public static final byte[] BASIC_QOS_OK;
    public static final byte[] BASIC_CONSUME;
    public static final byte[] BASIC_CONSUME_OK;
    public static final byte[] BASIC_CANCEL;
    public static final byte[] BASIC_CANCEL_OK;
    public static final byte[] BASIC_PUBLISH;
    public static final byte[] BASIC_RETURN;
    public static final byte[] BASIC_DELIVER;
    public static final byte[] BASIC_GET;
    public static final byte[] BASIC_GET_OK;
    public static final byte[] BASIC_GET_EMPTY;
    public static final byte[] BASIC_ACK;
    public static final byte[] BASIC_REJECT;
    public static final byte[] BASIC_RECOVER_ASYNC;
    public static final byte[] BASIC_RECOVER;
    public static final byte[] BASIC_RECOVER_OK;
    
    // Transaction frames
    public static final byte[] TX_SELECT;
    public static final byte[] TX_SELECT_OK;
    public static final byte[] TX_COMMIT;
    public static final byte[] TX_COMMIT_OK;
    public static final byte[] TX_ROLLBACK;
    public static final byte[] TX_ROLLBACK_OK;
    
    // Heartbeat frame
    public static final byte[] HEARTBEAT;
    
    static {
        try {
            // Initialize Connection frames
            CONNECTION_START = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.START, AMQPPayloadHeaderType.Connection.START, new ArrayList<>());
            CONNECTION_START_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.START_OK, AMQPPayloadHeaderType.Connection.START_OK, new ArrayList<>());
            CONNECTION_TUNE = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.TUNE, AMQPPayloadHeaderType.Connection.TUNE, new ArrayList<>());
            CONNECTION_TUNE_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.TUNE_OK, AMQPPayloadHeaderType.Connection.TUNE_OK, new ArrayList<>());
            CONNECTION_OPEN = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.OPEN, AMQPPayloadHeaderType.Connection.OPEN, new ArrayList<>());
            CONNECTION_OPEN_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.OPEN_OK, AMQPPayloadHeaderType.Connection.OPEN_OK, new ArrayList<>());
            CONNECTION_CLOSE = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.CLOSE, AMQPPayloadHeaderType.Connection.CLOSE, new ArrayList<>());
            CONNECTION_CLOSE_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.CLOSE_OK, AMQPPayloadHeaderType.Connection.CLOSE_OK, new ArrayList<>());
            
            // Initialize Channel frames
            CHANNEL_OPEN = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.OPEN, AMQPPayloadHeaderType.Flow.OPEN, new ArrayList<>());
            CHANNEL_OPEN_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.OPEN_OK, AMQPPayloadHeaderType.Flow.OPEN_OK, new ArrayList<>());
            CHANNEL_CLOSE = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.CLOSE, AMQPPayloadHeaderType.Flow.CLOSE, new ArrayList<>());
            CHANNEL_CLOSE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.CLOSE_OK, AMQPPayloadHeaderType.Flow.CLOSE_OK, new ArrayList<>());
            CHANNEL_FLOW = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.FLOW, AMQPPayloadHeaderType.Flow.FLOW, new ArrayList<>());
            CHANNEL_FLOW_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.FLOW_OK, AMQPPayloadHeaderType.Flow.FLOW_OK, new ArrayList<>());
            
            // Initialize Exchange frames
            EXCHANGE_DECLARE = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DECLARE, AMQPPayloadHeaderType.Exchange.DECLARE, new ArrayList<>());
            EXCHANGE_DECLARE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DECLARE_OK, AMQPPayloadHeaderType.Exchange.DECLARE_OK, new ArrayList<>());
            EXCHANGE_DELETE = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DELETE, AMQPPayloadHeaderType.Exchange.DELETE, new ArrayList<>());
            EXCHANGE_DELETE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DELETE_OK, AMQPPayloadHeaderType.Exchange.DELETE_OK, new ArrayList<>());
            
            // Initialize Queue frames
            QUEUE_DECLARE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DECLARE, AMQPPayloadHeaderType.Queue.DECLARE, new ArrayList<>());
            QUEUE_DECLARE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DECLARE_OK, AMQPPayloadHeaderType.Queue.DECLARE_OK, new ArrayList<>());
            QUEUE_BIND = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.BIND, AMQPPayloadHeaderType.Queue.BIND, new ArrayList<>());
            QUEUE_BIND_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.BIND_OK, AMQPPayloadHeaderType.Queue.BIND_OK, new ArrayList<>());
            QUEUE_UNBIND = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.UNBIND, AMQPPayloadHeaderType.Queue.UNBIND, new ArrayList<>());
            QUEUE_UNBIND_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.UNBIND_OK, AMQPPayloadHeaderType.Queue.UNBIND_OK, new ArrayList<>());
            QUEUE_PURGE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.PURGE, AMQPPayloadHeaderType.Queue.PURGE, new ArrayList<>());
            QUEUE_PURGE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.PURGE_OK, AMQPPayloadHeaderType.Queue.PURGE_OK, new ArrayList<>());
            QUEUE_DELETE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DELETE, AMQPPayloadHeaderType.Queue.DELETE, new ArrayList<>());
            QUEUE_DELETE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DELETE_OK, AMQPPayloadHeaderType.Queue.DELETE_OK, new ArrayList<>());
            
            // Initialize Basic frames
            BASIC_QOS = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS, AMQPPayloadHeaderType.Basic.QOS, new ArrayList<>());
            BASIC_QOS_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS_OK, AMQPPayloadHeaderType.Basic.QOS_OK, new ArrayList<>());
            BASIC_CONSUME = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CONSUME, AMQPPayloadHeaderType.Basic.CONSUME, new ArrayList<>());
            BASIC_CONSUME_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CONSUME_OK, AMQPPayloadHeaderType.Basic.CONSUME_OK, new ArrayList<>());
            BASIC_CANCEL = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CANCEL, AMQPPayloadHeaderType.Basic.CANCEL, new ArrayList<>());
            BASIC_CANCEL_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CANCEL_OK, AMQPPayloadHeaderType.Basic.CANCEL_OK, new ArrayList<>());
            BASIC_PUBLISH = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.PUBLISH, AMQPPayloadHeaderType.Basic.PUBLISH, new ArrayList<>());
            BASIC_RETURN = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RETURN, AMQPPayloadHeaderType.Basic.RETURN, new ArrayList<>());
            BASIC_DELIVER = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.DELIVER, AMQPPayloadHeaderType.Basic.DELIVER, new ArrayList<>());
            BASIC_GET = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET, AMQPPayloadHeaderType.Basic.GET, new ArrayList<>());
            BASIC_GET_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET_OK, AMQPPayloadHeaderType.Basic.GET_OK, new ArrayList<>());
            BASIC_GET_EMPTY = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET_EMPTY, AMQPPayloadHeaderType.Basic.GET_EMPTY, new ArrayList<>());
            BASIC_ACK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.ACK, AMQPPayloadHeaderType.Basic.ACK, new ArrayList<>());
            BASIC_REJECT = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.REJECT, AMQPPayloadHeaderType.Basic.REJECT, new ArrayList<>());
            BASIC_RECOVER_ASYNC = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER_ASYNC, AMQPPayloadHeaderType.Basic.RECOVER_ASYNC, new ArrayList<>());
            BASIC_RECOVER = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER, AMQPPayloadHeaderType.Basic.RECOVER, new ArrayList<>());
            BASIC_RECOVER_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER_OK, AMQPPayloadHeaderType.Basic.RECOVER_OK, new ArrayList<>());
            
            // Initialize Transaction frames
            TX_SELECT = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.SELECT, AMQPPayloadHeaderType.Tx.SELECT, new ArrayList<>());
            TX_SELECT_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.SELECT_OK, AMQPPayloadHeaderType.Tx.SELECT_OK, new ArrayList<>());
            TX_COMMIT = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.COMMIT, AMQPPayloadHeaderType.Tx.COMMIT, new ArrayList<>());
            TX_COMMIT_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.COMMIT_OK, AMQPPayloadHeaderType.Tx.COMMIT_OK, new ArrayList<>());
            TX_ROLLBACK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.ROLLBACK, AMQPPayloadHeaderType.Tx.ROLLBACK, new ArrayList<>());
            TX_ROLLBACK_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.ROLLBACK_OK, AMQPPayloadHeaderType.Tx.ROLLBACK_OK, new ArrayList<>());
            
            // Initialize Heartbeat frame
            HEARTBEAT = createHeartbeatFrame((short)0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize standard frames", e);
        }
    }
    
    /**
     * Creates a method frame with the specified properties.
     *
     * @param channel The channel number
     * @param method The method type
     * @param fields List of data fields
     * @return Byte array representing the frame
     * @throws IOException If frame creation fails
     */
    private static byte[] createMethodFrame(short channel, IFramePayloadClass classType, IFramePayloadMethod method, List<AMQPDataField> fields) throws IOException {
        return FrameFactory.methodFrame()
                .withChannel(channel)
                .withClass(classType)
                .withMethod(method)
                .build();
    }
    
    /**
     * Creates a heartbeat frame with the specified channel.
     *
     * @param channel The channel number
     * @return Byte array representing the frame
     * @throws IOException If frame creation fails
     */
    private static byte[] createHeartbeatFrame(short channel) throws IOException {
        return FrameFactory.heartbeatFrame()
                .withChannel(channel)
                .build();
    }
    
    /**
     * Creates a content header frame.
     *
     * @param channel The channel number
     * @param classId The class ID
     * @param bodySize The size of the content body
     * @return Byte array representing the frame
     * @throws IOException If frame creation fails
     */
    public static byte[] createContentHeaderFrame(short channel, short classId, long bodySize) throws IOException {
        return FrameFactory.headerFrame()
                .withChannel(channel)
                .withClassId(classId)
                .withBodySize(bodySize)
                .build();
    }
    
    /**
     * Creates a content body frame.
     *
     * @param channel The channel number
     * @param content The content data
     * @return Byte array representing the frame
     * @throws IOException If frame creation fails
     */
    public static byte[] createContentBodyFrame(short channel, byte[] content) throws IOException {
        return FrameFactory.bodyFrame()
                .withChannel(channel)
                .withContent(content)
                .build();
    }
}
