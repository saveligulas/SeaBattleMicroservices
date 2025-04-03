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
            List<AMQPDataField> connectionStartFields = new ArrayList<>();
            connectionStartFields.add(new AMQPDataField("version-major", (byte)0, AMQPDataType.OCTET)); // protocol major version
            connectionStartFields.add(new AMQPDataField("version-minor", (byte)9, AMQPDataType.OCTET)); // protocol minor version
            connectionStartFields.add(new AMQPDataField("server-properties", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // server properties
            connectionStartFields.add(new AMQPDataField("mechanisms", "PLAIN AMQPLAIN", AMQPDataType.LONG_STRING)); // security mechanisms
            connectionStartFields.add(new AMQPDataField("locales", "en_US", AMQPDataType.LONG_STRING)); // available message locales
            CONNECTION_START = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.START, AMQPPayloadHeaderType.Connection.START, connectionStartFields);
            List<AMQPDataField> connectionStartOkFields = new ArrayList<>();
            connectionStartOkFields.add(new AMQPDataField("client-properties", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // client properties
            connectionStartOkFields.add(new AMQPDataField("mechanism", "", AMQPDataType.SHORT_STRING)); // selected security mechanism
            connectionStartOkFields.add(new AMQPDataField("response", "", AMQPDataType.LONG_STRING)); // security response data
            connectionStartOkFields.add(new AMQPDataField("locale", "en_US", AMQPDataType.SHORT_STRING)); // selected message locale
            CONNECTION_START_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.START_OK, AMQPPayloadHeaderType.Connection.START_OK, connectionStartOkFields);
            List<AMQPDataField> connectionTuneFields = new ArrayList<>();
            connectionTuneFields.add(new AMQPDataField("channel-max", (short)0, AMQPDataType.SHORT)); // proposed maximum channels
            connectionTuneFields.add(new AMQPDataField("frame-max", 131072, AMQPDataType.LONG)); // proposed maximum frame size
            connectionTuneFields.add(new AMQPDataField("heartbeat", (short)0, AMQPDataType.SHORT)); // desired heartbeat delay
            CONNECTION_TUNE = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.TUNE, AMQPPayloadHeaderType.Connection.TUNE, connectionTuneFields);
            List<AMQPDataField> connectionTuneOkFields = new ArrayList<>();
            connectionTuneOkFields.add(new AMQPDataField("channel-max", (short)0, AMQPDataType.SHORT)); // negotiated maximum channels
            connectionTuneOkFields.add(new AMQPDataField("frame-max", 131072, AMQPDataType.LONG)); // negotiated maximum frame size
            connectionTuneOkFields.add(new AMQPDataField("heartbeat", (short)0, AMQPDataType.SHORT)); // desired heartbeat delay
            CONNECTION_TUNE_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.TUNE_OK, AMQPPayloadHeaderType.Connection.TUNE_OK, connectionTuneOkFields);
            List<AMQPDataField> connectionOpenFields = new ArrayList<>();
            connectionOpenFields.add(new AMQPDataField("virtual-host", "/", AMQPDataType.SHORT_STRING)); // virtual host name
            connectionOpenFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING)); // reserved
            connectionOpenFields.add(new AMQPDataField("reserved-2", false, AMQPDataType.BIT)); // reserved
            CONNECTION_OPEN = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.OPEN, AMQPPayloadHeaderType.Connection.OPEN, connectionOpenFields);
            List<AMQPDataField> connectionOpenOkFields = new ArrayList<>();
            connectionOpenOkFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING)); // reserved
            CONNECTION_OPEN_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.OPEN_OK, AMQPPayloadHeaderType.Connection.OPEN_OK, connectionOpenOkFields);
            List<AMQPDataField> connectionCloseFields = new ArrayList<>();
            connectionCloseFields.add(new AMQPDataField("reply-code", (short)0, AMQPDataType.SHORT)); // reply code
            connectionCloseFields.add(new AMQPDataField("reply-text", "", AMQPDataType.SHORT_STRING)); // reply text
            connectionCloseFields.add(new AMQPDataField("class-id", (short)0, AMQPDataType.SHORT)); // failing method class
            connectionCloseFields.add(new AMQPDataField("method-id", (short)0, AMQPDataType.SHORT)); // failing method ID
            CONNECTION_CLOSE = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.CLOSE, AMQPPayloadHeaderType.Connection.CLOSE, connectionCloseFields);
            // No fields in CONNECTION_CLOSE_OK
            CONNECTION_CLOSE_OK = createMethodFrame((short)0, AMQPPayloadHeaderType.Connection.CLOSE_OK, AMQPPayloadHeaderType.Connection.CLOSE_OK, new ArrayList<>());
            
            // Initialize Channel frames
            List<AMQPDataField> channelOpenFields = new ArrayList<>();
            channelOpenFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING)); // reserved
            CHANNEL_OPEN = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.OPEN, AMQPPayloadHeaderType.Flow.OPEN, channelOpenFields);
            List<AMQPDataField> channelOpenOkFields = new ArrayList<>();
            channelOpenOkFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.LONG_STRING)); // reserved
            CHANNEL_OPEN_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.OPEN_OK, AMQPPayloadHeaderType.Flow.OPEN_OK, channelOpenOkFields);
            List<AMQPDataField> channelCloseFields = new ArrayList<>();
            channelCloseFields.add(new AMQPDataField("reply-code", (short)0, AMQPDataType.SHORT)); // reply code
            channelCloseFields.add(new AMQPDataField("reply-text", "", AMQPDataType.SHORT_STRING)); // reply text
            channelCloseFields.add(new AMQPDataField("class-id", (short)0, AMQPDataType.SHORT)); // failing method class
            channelCloseFields.add(new AMQPDataField("method-id", (short)0, AMQPDataType.SHORT)); // failing method ID
            CHANNEL_CLOSE = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.CLOSE, AMQPPayloadHeaderType.Flow.CLOSE, channelCloseFields);
            // No fields in CHANNEL_CLOSE_OK
            CHANNEL_CLOSE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.CLOSE_OK, AMQPPayloadHeaderType.Flow.CLOSE_OK, new ArrayList<>());
            List<AMQPDataField> channelFlowFields = new ArrayList<>();
            channelFlowFields.add(new AMQPDataField("active", true, AMQPDataType.BIT)); // start/stop content frames
            CHANNEL_FLOW = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.FLOW, AMQPPayloadHeaderType.Flow.FLOW, channelFlowFields);
            List<AMQPDataField> channelFlowOkFields = new ArrayList<>();
            channelFlowOkFields.add(new AMQPDataField("active", true, AMQPDataType.BIT)); // current flow setting
            CHANNEL_FLOW_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Flow.FLOW_OK, AMQPPayloadHeaderType.Flow.FLOW_OK, channelFlowOkFields);
            
            // Initialize Exchange frames
            List<AMQPDataField> exchangeDeclareFields = new ArrayList<>();
            exchangeDeclareFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            exchangeDeclareFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            exchangeDeclareFields.add(new AMQPDataField("type", "direct", AMQPDataType.SHORT_STRING)); // exchange type
            exchangeDeclareFields.add(new AMQPDataField("passive", false, AMQPDataType.BIT)); // do not create exchange
            exchangeDeclareFields.add(new AMQPDataField("durable", false, AMQPDataType.BIT)); // request a durable exchange
            exchangeDeclareFields.add(new AMQPDataField("reserved-2", false, AMQPDataType.BIT)); // reserved
            exchangeDeclareFields.add(new AMQPDataField("reserved-3", false, AMQPDataType.BIT)); // reserved
            exchangeDeclareFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            exchangeDeclareFields.add(new AMQPDataField("arguments", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // arguments for declaration
            EXCHANGE_DECLARE = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DECLARE, AMQPPayloadHeaderType.Exchange.DECLARE, exchangeDeclareFields);
            // No fields in EXCHANGE_DECLARE_OK
            EXCHANGE_DECLARE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DECLARE_OK, AMQPPayloadHeaderType.Exchange.DECLARE_OK, new ArrayList<>());
            List<AMQPDataField> exchangeDeleteFields = new ArrayList<>();
            exchangeDeleteFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            exchangeDeleteFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            exchangeDeleteFields.add(new AMQPDataField("if-unused", false, AMQPDataType.BIT)); // delete only if unused
            exchangeDeleteFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            EXCHANGE_DELETE = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DELETE, AMQPPayloadHeaderType.Exchange.DELETE, exchangeDeleteFields);
            // No fields in EXCHANGE_DELETE_OK
            EXCHANGE_DELETE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Exchange.DELETE_OK, AMQPPayloadHeaderType.Exchange.DELETE_OK, new ArrayList<>());
            
            // Initialize Queue frames
            List<AMQPDataField> queueDeclareFields = new ArrayList<>();
            queueDeclareFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            queueDeclareFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queueDeclareFields.add(new AMQPDataField("passive", false, AMQPDataType.BIT)); // do not create queue
            queueDeclareFields.add(new AMQPDataField("durable", false, AMQPDataType.BIT)); // request a durable queue
            queueDeclareFields.add(new AMQPDataField("exclusive", false, AMQPDataType.BIT)); // request an exclusive queue
            queueDeclareFields.add(new AMQPDataField("auto-delete", false, AMQPDataType.BIT)); // auto-delete queue when unused
            queueDeclareFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            queueDeclareFields.add(new AMQPDataField("arguments", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // arguments for declaration
            QUEUE_DECLARE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DECLARE, AMQPPayloadHeaderType.Queue.DECLARE, queueDeclareFields);
            List<AMQPDataField> queueDeclareOkFields = new ArrayList<>();
            queueDeclareOkFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queueDeclareOkFields.add(new AMQPDataField("message-count", 0, AMQPDataType.LONG)); // message count
            queueDeclareOkFields.add(new AMQPDataField("consumer-count", 0, AMQPDataType.LONG)); // consumer count
            QUEUE_DECLARE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DECLARE_OK, AMQPPayloadHeaderType.Queue.DECLARE_OK, queueDeclareOkFields);
            List<AMQPDataField> queueBindFields = new ArrayList<>();
            queueBindFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            queueBindFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queueBindFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            queueBindFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // message routing key
            queueBindFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            queueBindFields.add(new AMQPDataField("arguments", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // arguments for binding
            QUEUE_BIND = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.BIND, AMQPPayloadHeaderType.Queue.BIND, queueBindFields);
            // No fields in QUEUE_BIND_OK
            QUEUE_BIND_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.BIND_OK, AMQPPayloadHeaderType.Queue.BIND_OK, new ArrayList<>());
            List<AMQPDataField> queueUnbindFields = new ArrayList<>();
            queueUnbindFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            queueUnbindFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queueUnbindFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            queueUnbindFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // routing key of binding
            queueUnbindFields.add(new AMQPDataField("arguments", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // arguments of binding
            QUEUE_UNBIND = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.UNBIND, AMQPPayloadHeaderType.Queue.UNBIND, queueUnbindFields);
            // No fields in QUEUE_UNBIND_OK
            QUEUE_UNBIND_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.UNBIND_OK, AMQPPayloadHeaderType.Queue.UNBIND_OK, new ArrayList<>());
            List<AMQPDataField> queuePurgeFields = new ArrayList<>();
            queuePurgeFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            queuePurgeFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queuePurgeFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            QUEUE_PURGE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.PURGE, AMQPPayloadHeaderType.Queue.PURGE, queuePurgeFields);
            List<AMQPDataField> queuePurgeOkFields = new ArrayList<>();
            queuePurgeOkFields.add(new AMQPDataField("message-count", 0, AMQPDataType.LONG)); // message count
            QUEUE_PURGE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.PURGE_OK, AMQPPayloadHeaderType.Queue.PURGE_OK, queuePurgeOkFields);
            List<AMQPDataField> queueDeleteFields = new ArrayList<>();
            queueDeleteFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            queueDeleteFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            queueDeleteFields.add(new AMQPDataField("if-unused", false, AMQPDataType.BIT)); // delete only if unused
            queueDeleteFields.add(new AMQPDataField("if-empty", false, AMQPDataType.BIT)); // delete only if empty
            queueDeleteFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            QUEUE_DELETE = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DELETE, AMQPPayloadHeaderType.Queue.DELETE, queueDeleteFields);
            List<AMQPDataField> queueDeleteOkFields = new ArrayList<>();
            queueDeleteOkFields.add(new AMQPDataField("message-count", 0, AMQPDataType.LONG)); // message count
            QUEUE_DELETE_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Queue.DELETE_OK, AMQPPayloadHeaderType.Queue.DELETE_OK, queueDeleteOkFields);
            
            // Initialize Basic frames
            List<AMQPDataField> basicQosFields = new ArrayList<>();
            basicQosFields.add(new AMQPDataField("prefetch-size", 0, AMQPDataType.LONG)); // prefetch window in octets
            basicQosFields.add(new AMQPDataField("prefetch-count", (short)0, AMQPDataType.SHORT)); // prefetch window in messages
            basicQosFields.add(new AMQPDataField("global", false, AMQPDataType.BIT)); // apply to entire connection
            BASIC_QOS = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS, AMQPPayloadHeaderType.Basic.QOS, basicQosFields);
            // No fields in BASIC_QOS_OK
            BASIC_QOS_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS_OK, AMQPPayloadHeaderType.Basic.QOS_OK, new ArrayList<>());
            List<AMQPDataField> basicConsumeFields = new ArrayList<>();
            basicConsumeFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            basicConsumeFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            basicConsumeFields.add(new AMQPDataField("consumer-tag", "", AMQPDataType.SHORT_STRING)); // consumer tag
            basicConsumeFields.add(new AMQPDataField("no-local", false, AMQPDataType.BIT)); // no-local
            basicConsumeFields.add(new AMQPDataField("no-ack", false, AMQPDataType.BIT)); // no-ack
            basicConsumeFields.add(new AMQPDataField("exclusive", false, AMQPDataType.BIT)); // request exclusive access
            basicConsumeFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            basicConsumeFields.add(new AMQPDataField("arguments", new HashMap<>(), AMQPDataType.FIELD_TABLE)); // arguments for declaration
            BASIC_CONSUME = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CONSUME, AMQPPayloadHeaderType.Basic.CONSUME, basicConsumeFields);
            List<AMQPDataField> basicConsumeOkFields = new ArrayList<>();
            basicConsumeOkFields.add(new AMQPDataField("consumer-tag", "", AMQPDataType.SHORT_STRING)); // consumer tag
            BASIC_CONSUME_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CONSUME_OK, AMQPPayloadHeaderType.Basic.CONSUME_OK, basicConsumeOkFields);
            List<AMQPDataField> basicCancelFields = new ArrayList<>();
            basicCancelFields.add(new AMQPDataField("consumer-tag", "", AMQPDataType.SHORT_STRING)); // consumer tag
            basicCancelFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT)); // no-wait
            BASIC_CANCEL = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CANCEL, AMQPPayloadHeaderType.Basic.CANCEL, basicCancelFields);
            List<AMQPDataField> basicCancelOkFields = new ArrayList<>();
            basicCancelOkFields.add(new AMQPDataField("consumer-tag", "", AMQPDataType.SHORT_STRING)); // consumer tag
            BASIC_CANCEL_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.CANCEL_OK, AMQPPayloadHeaderType.Basic.CANCEL_OK, basicCancelOkFields);
            List<AMQPDataField> basicPublishFields = new ArrayList<>();
            basicPublishFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            basicPublishFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            basicPublishFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // message routing key
            basicPublishFields.add(new AMQPDataField("mandatory", false, AMQPDataType.BIT)); // indicate mandatory routing
            basicPublishFields.add(new AMQPDataField("immediate", false, AMQPDataType.BIT)); // request immediate delivery
            BASIC_PUBLISH = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.PUBLISH, AMQPPayloadHeaderType.Basic.PUBLISH, basicPublishFields);
            List<AMQPDataField> basicReturnFields = new ArrayList<>();
            basicReturnFields.add(new AMQPDataField("reply-code", (short)0, AMQPDataType.SHORT)); // reply code
            basicReturnFields.add(new AMQPDataField("reply-text", "", AMQPDataType.SHORT_STRING)); // reply text
            basicReturnFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            basicReturnFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // message routing key
            BASIC_RETURN = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RETURN, AMQPPayloadHeaderType.Basic.RETURN, basicReturnFields);
            List<AMQPDataField> basicDeliverFields = new ArrayList<>();
            basicDeliverFields.add(new AMQPDataField("consumer-tag", "", AMQPDataType.SHORT_STRING)); // consumer tag
            basicDeliverFields.add(new AMQPDataField("delivery-tag", 0L, AMQPDataType.LONGLONG)); // delivery tag
            basicDeliverFields.add(new AMQPDataField("redelivered", false, AMQPDataType.BIT)); // redelivered flag
            basicDeliverFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            basicDeliverFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // message routing key
            BASIC_DELIVER = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.DELIVER, AMQPPayloadHeaderType.Basic.DELIVER, basicDeliverFields);
            List<AMQPDataField> basicGetFields = new ArrayList<>();
            basicGetFields.add(new AMQPDataField("reserved-1", (short)0, AMQPDataType.SHORT)); // reserved
            basicGetFields.add(new AMQPDataField("queue", "", AMQPDataType.SHORT_STRING)); // queue name
            basicGetFields.add(new AMQPDataField("no-ack", false, AMQPDataType.BIT)); // no-ack
            BASIC_GET = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET, AMQPPayloadHeaderType.Basic.GET, basicGetFields);
            List<AMQPDataField> basicGetOkFields = new ArrayList<>();
            basicGetOkFields.add(new AMQPDataField("delivery-tag", 0L, AMQPDataType.LONGLONG)); // delivery tag
            basicGetOkFields.add(new AMQPDataField("redelivered", false, AMQPDataType.BIT)); // redelivered flag
            basicGetOkFields.add(new AMQPDataField("exchange", "", AMQPDataType.SHORT_STRING)); // exchange name
            basicGetOkFields.add(new AMQPDataField("routing-key", "", AMQPDataType.SHORT_STRING)); // message routing key
            basicGetOkFields.add(new AMQPDataField("message-count", 0, AMQPDataType.LONG)); // message count
            BASIC_GET_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET_OK, AMQPPayloadHeaderType.Basic.GET_OK, basicGetOkFields);
            List<AMQPDataField> basicGetEmptyFields = new ArrayList<>();
            basicGetEmptyFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING)); // reserved
            BASIC_GET_EMPTY = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.GET_EMPTY, AMQPPayloadHeaderType.Basic.GET_EMPTY, basicGetEmptyFields);
            List<AMQPDataField> basicAckFields = new ArrayList<>();
            basicAckFields.add(new AMQPDataField("delivery-tag", 0L, AMQPDataType.LONGLONG)); // delivery tag
            basicAckFields.add(new AMQPDataField("multiple", false, AMQPDataType.BIT)); // acknowledge multiple messages
            BASIC_ACK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.ACK, AMQPPayloadHeaderType.Basic.ACK, basicAckFields);
            List<AMQPDataField> basicRejectFields = new ArrayList<>();
            basicRejectFields.add(new AMQPDataField("delivery-tag", 0L, AMQPDataType.LONGLONG)); // delivery tag
            basicRejectFields.add(new AMQPDataField("requeue", false, AMQPDataType.BIT)); // requeue the message
            BASIC_REJECT = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.REJECT, AMQPPayloadHeaderType.Basic.REJECT, basicRejectFields);
            List<AMQPDataField> basicRecoverAsyncFields = new ArrayList<>();
            basicRecoverAsyncFields.add(new AMQPDataField("requeue", false, AMQPDataType.BIT)); // requeue the message
            BASIC_RECOVER_ASYNC = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER_ASYNC, AMQPPayloadHeaderType.Basic.RECOVER_ASYNC, basicRecoverAsyncFields);
            List<AMQPDataField> basicRecoverFields = new ArrayList<>();
            basicRecoverFields.add(new AMQPDataField("requeue", false, AMQPDataType.BIT)); // requeue the message
            BASIC_RECOVER = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER, AMQPPayloadHeaderType.Basic.RECOVER, basicRecoverFields);
            // No fields in BASIC_RECOVER_OK
            BASIC_RECOVER_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.RECOVER_OK, AMQPPayloadHeaderType.Basic.RECOVER_OK, new ArrayList<>());
            
            // Initialize Transaction frames
            // No fields in TX_SELECT
            TX_SELECT = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.SELECT, AMQPPayloadHeaderType.Tx.SELECT, new ArrayList<>());
            // No fields in TX_SELECT_OK
            TX_SELECT_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.SELECT_OK, AMQPPayloadHeaderType.Tx.SELECT_OK, new ArrayList<>());
            // No fields in TX_COMMIT
            TX_COMMIT = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.COMMIT, AMQPPayloadHeaderType.Tx.COMMIT, new ArrayList<>());
            // No fields in TX_COMMIT_OK
            TX_COMMIT_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.COMMIT_OK, AMQPPayloadHeaderType.Tx.COMMIT_OK, new ArrayList<>());
            // No fields in TX_ROLLBACK
            TX_ROLLBACK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.ROLLBACK, AMQPPayloadHeaderType.Tx.ROLLBACK, new ArrayList<>());
            // No fields in TX_ROLLBACK_OK
            TX_ROLLBACK_OK = createMethodFrame((short)1, AMQPPayloadHeaderType.Tx.ROLLBACK_OK, AMQPPayloadHeaderType.Tx.ROLLBACK_OK, new ArrayList<>());
            
            // Initialize Heartbeat frame - heartbeat frames have no fields
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
