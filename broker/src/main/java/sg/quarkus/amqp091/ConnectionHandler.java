package sg.quarkus.amqp091;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.protocol.frame.AMQPPayloadHeaderType;
import sg.quarkus.amqp091.protocol.frame.FrameFactory;
import sg.quarkus.amqp091.protocol.frame.Header;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class ConnectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    
    private final NetSocket socket;
    private ConnectionState state = ConnectionState.INITIAL;
    private final Map<Short, Channel> channels = new ConcurrentHashMap<>();
    
    public enum ConnectionState {
        INITIAL,
        PROTOCOL_HEADER_RECEIVED,
        START_SENT,
        START_OK_RECEIVED,
        TUNE_SENT,
        TUNE_OK_RECEIVED,
        OPEN_SENT,
        OPEN_RECEIVED,
        OPEN_OK_RECEIVED,
        CONNECTED,
        CLOSED
    }
    
    public ConnectionHandler(NetSocket socket) {
        this.socket = socket;
    }
    
    public void handleData(Buffer buffer) {
        byte[] data = buffer.getBytes();
        logger.debug("Received data: {}", bytesToHex(data));
        
        try {
            // Special case for INITIAL state - protocol header doesn't have a frame end marker
            if (state == ConnectionState.INITIAL) {
                handleProtocolHeader(data);
                return;
            }
            
            // Handle multiple frames in a single buffer
            int offset = 0;
            while (offset < data.length) {
                // If there's not enough data for a complete frame header, wait for more data
                if (offset + 7 > data.length) {
                    logger.warn("Incomplete frame header received");
                    break;
                }
                
                // Extract frame size from the header (bytes 3-6)
                int frameSize = ((data[offset + 3] & 0xff) << 24) | 
                               ((data[offset + 4] & 0xff) << 16) | 
                               ((data[offset + 5] & 0xff) << 8) | 
                               (data[offset + 6] & 0xff);
                
                // Total frame size = 7 bytes (header) + payload size + 1 byte (end marker)
                int totalFrameSize = 7 + frameSize + 1;
                
                // If the buffer doesn't contain the complete frame, wait for more data
                if (offset + totalFrameSize > data.length) {
                    logger.warn("Incomplete frame received");
                    break;
                }
                
                // Extract the current frame
                byte[] frameData = new byte[totalFrameSize];
                System.arraycopy(data, offset, frameData, 0, totalFrameSize);
                
                // Process the frame based on current state
                processFrame(frameData);
                
                // Move to the next frame
                offset += totalFrameSize;
            }
        } catch (Exception e) {
            logger.error("Error handling frame", e);
            closeConnection();
        }
    }
    
    private void processFrame(byte[] frameData) {
        logger.debug("Processing frame: {}", bytesToHex(frameData));
        
        // Extract frame type and class/method IDs for method frames
        byte frameType = frameData[0];
        
        if (frameType == 1) { // Method frame
            short classId = (short) ((frameData[7] & 0xff) << 8 | (frameData[8] & 0xff));
            short methodId = (short) ((frameData[9] & 0xff) << 8 | (frameData[10] & 0xff));
            
            logger.debug("Method frame: class={}, method={}", classId, methodId);
            
            // Connection class (ID: 10)
            if (classId == 10) {
                switch (methodId) {
                    case 11: // Connection.Start-Ok
                        if (state == ConnectionState.START_SENT) {
                            handleStartOk(frameData);
                        } else {
                            logger.warn("Unexpected Connection.Start-Ok in state {}", state);
                        }
                        break;
                    case 31: // Connection.Tune-Ok
                        if (state == ConnectionState.TUNE_SENT) {
                            handleTuneOk(frameData);
                        } else {
                            logger.warn("Unexpected Connection.Tune-Ok in state {}", state);
                        }
                        break;
                    case 40: // Connection.Open
                        if (state == ConnectionState.TUNE_OK_RECEIVED) {
                            handleOpen(frameData);
                        } else {
                            logger.warn("Unexpected Connection.Open in state {}", state);
                        }
                        break;
                    case 41: // Connection.Open-Ok
                        if (state == ConnectionState.OPEN_SENT) {
                            handleOpenOk(frameData);
                        } else {
                            logger.warn("Unexpected Connection.Open-Ok in state {}", state);
                        }
                        break;
                    default:
                        logger.warn("Unhandled method ID for Connection class: {}", methodId);
                }
            } else {
                // Handle other classes when connected
                if (state == ConnectionState.CONNECTED) {
                    handleConnectedFrame(frameData);
                } else {
                    logger.warn("Unexpected class ID {} in state {}", classId, state);
                }
            }
        } else {
            // Handle other frame types (header, body, heartbeat)
            logger.debug("Non-method frame received: type={}", frameType);
            if (state == ConnectionState.CONNECTED) {
                handleConnectedFrame(frameData);
            } else {
                logger.warn("Unexpected frame type {} in state {}", frameType, state);
            }
        }
    }
    
    private void handleProtocolHeader(byte[] data) {
        if (Arrays.equals(data, Header.PROTOCOL_HEADER)) {
            logger.info("Received valid protocol header");
            state = ConnectionState.PROTOCOL_HEADER_RECEIVED;
            sendConnectionStart();
        } else {
            logger.error("Invalid protocol header");
            closeConnection();
        }
    }
    
    private void handleStartOk(byte[] frameData) {
        logger.info("Received Connection.Start-Ok frame");
        state = ConnectionState.START_OK_RECEIVED;
        sendConnectionTune();
    }
    
    private void handleTuneOk(byte[] frameData) {
        logger.info("Received Connection.Tune-Ok frame");
        state = ConnectionState.TUNE_OK_RECEIVED;
        // We no longer send Connection.Open here
        // Wait for client's Connection.Open instead
    }
    
    private void handleOpen(byte[] frameData) {
        logger.info("Received Connection.Open frame");
        state = ConnectionState.OPEN_RECEIVED;
        sendConnectionOpenOk();
    }
    
    private void handleOpenOk(byte[] frameData) {
        logger.info("Received Connection.Open-Ok frame");
        state = ConnectionState.CONNECTED;
        logger.info("AMQP connection established");
    }
    
    private void sendConnectionOpenOk() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel((short) 0)
                .withClass(AMQPPayloadHeaderType.Connection.OPEN_OK)
                .withMethod(AMQPPayloadHeaderType.Connection.OPEN_OK)
                .withShortStringField("reserved-1", "")
                .build();
            
            socket.write(Buffer.buffer(frame));
            state = ConnectionState.CONNECTED;
            logger.info("Sent Connection.Open-Ok frame");
            logger.info("AMQP connection established");
        } catch (IOException e) {
            logger.error("Error sending Connection.Open-Ok frame", e);
            closeConnection();
        }
    }
    
    private void handleConnectedFrame(byte[] frameData) {
        logger.debug("Received frame in CONNECTED state");
        
        byte frameType = frameData[0];
        short channelId = (short) ((frameData[1] & 0xff) << 8 | (frameData[2] & 0xff));
        
        if (frameType == 1) { // Method frame
            short classId = (short) ((frameData[7] & 0xff) << 8 | (frameData[8] & 0xff));
            short methodId = (short) ((frameData[9] & 0xff) << 8 | (frameData[10] & 0xff));
            
            logger.debug("Method frame on channel {}: class={}, method={}", channelId, classId, methodId);
            
            // Channel class (ID: 20)
            if (classId == 20) {
                switch (methodId) {
                    case 10: // Channel.Open
                        handleChannelOpen(channelId, frameData);
                        break;
                    case 40: // Channel.Close
                        handleChannelClose(channelId, frameData);
                        break;
                    default:
                        logger.warn("Unhandled method ID {} for Channel class", methodId);
                }
            } else if (channels.containsKey(channelId)) {
                // Forward to the appropriate channel handler
                Channel channel = channels.get(channelId);
                channel.handleFrame(frameData);
            } else {
                logger.warn("Received frame for unknown channel: {}", channelId);
            }
        } else {
            // Handle other frame types (header, body, heartbeat)
            if (channels.containsKey(channelId)) {
                Channel channel = channels.get(channelId);
                channel.handleFrame(frameData);
            } else if (channelId == 0) {
                // Connection-level non-method frames (like heartbeats)
                if (frameType == 8) { // Heartbeat
                    // Echo heartbeat back
                    socket.write(Buffer.buffer(frameData));
                    logger.debug("Heartbeat echoed back");
                }
            } else {
                logger.warn("Received non-method frame for unknown channel: {}", channelId);
            }
        }
    }
    
    private void sendConnectionStart() {
        try {
            Map<String, Object> serverProperties = new HashMap<>();
            serverProperties.put("product", "MyAMQPBroker");
            serverProperties.put("version", "1.0.0");
            serverProperties.put("platform", "Java");
            
            byte[] frame = FrameFactory.methodFrame()
                .withChannel((short) 0)
                .withClass(AMQPPayloadHeaderType.Connection.START)
                .withMethod(AMQPPayloadHeaderType.Connection.START)
                .withOctetField("version-major", (byte) 0)
                .withOctetField("version-minor", (byte) 9)
                .withTableField("server-properties", serverProperties)
                .withLongStringField("mechanisms", "PLAIN")
                .withLongStringField("locales", "en_US")
                .build();
            
            socket.write(Buffer.buffer(frame));
            state = ConnectionState.START_SENT;
            logger.info("Sent Connection.Start frame");
        } catch (IOException e) {
            logger.error("Error sending Connection.Start frame", e);
            closeConnection();
        }
    }
    
    private void sendConnectionTune() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel((short) 0)
                .withClass(AMQPPayloadHeaderType.Connection.TUNE)
                .withMethod(AMQPPayloadHeaderType.Connection.TUNE)
                .withShortField("channel-max", (short) 0) // No limit
                .withIntField("frame-max", 131072) // 128K default
                .withShortField("heartbeat", (short) 60) // 60 second heartbeat
                .build();
            
            socket.write(Buffer.buffer(frame));
            state = ConnectionState.TUNE_SENT;
            logger.info("Sent Connection.Tune frame");
        } catch (IOException e) {
            logger.error("Error sending Connection.Tune frame", e);
            closeConnection();
        }
    }
    
    private void sendConnectionOpen() {
        try {
            byte[] frame = FrameFactory.methodFrame()
                .withChannel((short) 0)
                .withClass(AMQPPayloadHeaderType.Connection.OPEN)
                .withMethod(AMQPPayloadHeaderType.Connection.OPEN)
                .withShortStringField("virtual-host", "/")// '/' default in rabbitmq
                .withShortStringField("reserved-1", "")
                .withBitField("reserved-2", false)
                .build();
            
            socket.write(Buffer.buffer(frame));
            state = ConnectionState.OPEN_SENT;
            logger.info("Sent Connection.Open frame");
        } catch (IOException e) {
            logger.error("Error sending Connection.Open frame", e);
            closeConnection();
        }
    }
    
    private void closeConnection() {
        try {
            if (state != ConnectionState.CLOSED) {
                socket.close();
                state = ConnectionState.CLOSED;
                logger.info("Connection closed");
            }
        } catch (Exception e) {
            logger.error("Error closing connection", e);
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    /**
     * Handles a Channel.Open frame and creates a new channel.
     * 
     * @param channelId The channel ID
     * @param frameData The raw frame data
     */
    private void handleChannelOpen(short channelId, byte[] frameData) {
        logger.info("Received Channel.Open for channel {}", channelId);
        
        // Create new channel if it doesn't exist
        if (!channels.containsKey(channelId)) {
            Channel channel = new Channel(channelId, socket);
            channels.put(channelId, channel);
            channel.open();
        } else {
            logger.warn("Channel {} already exists", channelId);
        }
    }
    
    /**
     * Handles a Channel.Close frame and closes the channel.
     * 
     * @param channelId The channel ID
     * @param frameData The raw frame data
     */
    private void handleChannelClose(short channelId, byte[] frameData) {
        logger.info("Received Channel.Close for channel {}", channelId);
        
        // Close channel if it exists
        if (channels.containsKey(channelId)) {
            Channel channel = channels.get(channelId);
            channel.close();
            channels.remove(channelId);
        } else {
            logger.warn("Attempt to close non-existent channel {}", channelId);
        }
    }
}
