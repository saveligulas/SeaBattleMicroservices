package sg.quarkus.amqp091;

import io.quarkus.runtime.Startup;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sg.quarkus.amqp091.protocol.frame.Frame;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Startup
public class AMQPServer {
    private static final Logger logger = LoggerFactory.getLogger(AMQPServer.class);
    private final Vertx vertx;
    private final Map<NetSocket, AMQPConnection> connections = new HashMap<>();

    private class AMQPConnection {
        private ConnectionState state = ConnectionState.INITIAL;
        private final NetSocket socket;
        private int protocolHeaderReceivedCount = 0;

        private enum ConnectionState {
            INITIAL,
            HEADER_RECEIVED,
            START_SENT,
            START_OK_RECEIVED,
            TUNE_SENT,
            OPEN_SENT,
            CONNECTED,
            CLOSED
        }

        public AMQPConnection(NetSocket socket) {
            this.socket = socket;
        }

        public void handleIncomingData(Buffer buffer) {
            byte[] received = buffer.getBytes();
            logger.debug("Received (hex): {}", bytesToHex(received));
            logger.debug("Current state: {}", state);
            logger.debug("Received Frame: {}", new Frame(received));

            switch (state) {
                case INITIAL:
                case HEADER_RECEIVED:
                    handleProtocolHeader(received);
                    break;
                case START_SENT:
                    handleStartOkResponse(received);
                    break;
                case TUNE_SENT:
                    handleTuneOkResponse(received);
                    break;
                default:
                    logger.warn("Unexpected state or frame: {}", state);
            }
        }

        private void handleProtocolHeader(byte[] received) {
            if (isValidProtocolHeader(received)) {
                protocolHeaderReceivedCount++;

                if (protocolHeaderReceivedCount > 3) {
                    logger.error("Too many protocol headers received. Closing connection.");
                    socket.close();
                    return;
                }

                if (state != ConnectionState.START_SENT) {
                    byte[] connectionStartFrame = createConnectionStartFrame();
                    socket.write(Buffer.buffer(connectionStartFrame));
                    logger.info("Sent Connection.Start frame.");
                    state = ConnectionState.START_SENT;
                }
            } else {
                logger.error("Invalid AMQP protocol header");
                socket.close();
            }
        }

        private void handleStartOkResponse(byte[] received) {
            logger.debug("Received Start-Ok frame: {}", bytesToHex(received));

            logger.info("Received valid Connection.Start-Ok frame");

            byte[] connectionTuneFrame = createConnectionTuneFrame();
            socket.write(Buffer.buffer(connectionTuneFrame));
            logger.info("Sent Connection.Tune frame.");
            state = ConnectionState.TUNE_SENT;


        }

        private void handleTuneOkResponse(byte[] received) {
            logger.debug("Received Tune-Ok frame: {}", bytesToHex(received));
            if (isConnectionTuneOkFrame(received)) {
                byte[] connectionOpenFrame = createConnectionOpenFrame();
                socket.write(Buffer.buffer(connectionOpenFrame));
                logger.info("Sent Connection.Open frame.");
                state = ConnectionState.CONNECTED;
            } else {
                logger.error("Invalid Connection.Tune-Ok frame, closing connection.");
                socket.close();
            }
        }

        private boolean isValidProtocolHeader(byte[] received) {
            byte[] AMQP_PROTOCOL_HEADER = {
                    'A', 'M', 'Q', 'P',
                    0x00, 0x00, 0x09, 0x01
            };
            return received.length == AMQP_PROTOCOL_HEADER.length &&
                    Arrays.equals(received, AMQP_PROTOCOL_HEADER);
        }

        private boolean isConnectionStartOkFrame(byte[] received) {
            return received.length > 10 &&
                    received[0] == 0x01 &&
                    received[3] == 0x00 &&
                    received[4] == 0x00 && received[5] == 0x0A &&
                    received[6] == 0x00 && received[7] == 0x0B;
        }

        private boolean isConnectionTuneOkFrame(byte[] received) {
            return received.length > 10 &&
                    received[0] == 0x01 &&
                    received[3] == 0x00 &&
                    received[4] == 0x00 && received[5] == 0x0A &&
                    received[6] == 0x00 && received[7] == 0x0F;
        }

        private byte[] createConnectionStartFrame() {
            return new byte[] {
                    // Frame Header (7 octets)
                    (byte) 0x01,             // Frame Type: Method (1) [1, 2]
                    (byte) 0x00, (byte) 0x00, // Channel: 0 (connection-level) [3, 4]
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x31, // Frame Size: 49 (payload size, excluding frame-end)

                    // Method Payload
                    // Class ID (short integer) [4-6]
                    (byte) 0x00, (byte) 0x0A, // 10 (connection) [7]
                    // Method ID (short integer) [4-6]
                    (byte) 0x00, (byte) 0x0A, // 10 (start) [7, 8]

                    // version-major (octet) [9]
                    (byte) 0x00,             // 0

                    // version-minor (octet) [10]
                    (byte) 0x09,             // 9

                    // server-properties (peer-properties table) [10, 11]
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x13, // Table Size: 19 bytes
                    (byte) 0x07, (byte) 'p', (byte) 'r', (byte) 'o', (byte) 'd', (byte) 'u', (byte) 'c', (byte) 't', // Short string: "product"
                    (byte) 0x53,                                                                                  // Field value type: long string ('S') [12]
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 'M', (byte) 'y', (byte) 'S', (byte) 'e', (byte) 'r', (byte) 'v', (byte) 'e', (byte) 'r', // Long string: "MyServer"

                    // mechanisms (longstr) [13, 14]
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, // String Length: 5
                    (byte) 'P', (byte) 'L', (byte) 'A', (byte) 'I', (byte) 'N', // String: "PLAIN"

                    // locales (longstr) [13, 14]
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x05, // String Length: 5
                    (byte) 'e', (byte) 'n', (byte) '_', (byte) 'U', (byte) 'S', // String: "en_US"

                    // Frame End (octet) [3, 12, 15]
                    (byte) 0xCE
            };
        }

        private byte[] createConnectionTuneFrame() {
            return new byte[]{
                    0x01,
                    0x00, 0x00,
                    0x00, 0x00, 0x00, 0x0C,
                    0x00, 0x0A, // class Connection
                    0x00, 0x1E, // method tune
                    0x00, 0x00, //frame-max
                    0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00,
                    (byte) 0xCE
            };
        }

        private byte[] createConnectionOpenFrame() {
            return new byte[]{
                    0x01,
                    0x00, 0x00,
                    0x00, 0x00, 0x00, 0x0C,
                    0x00, 0x0A,
                    0x00, 0x28,
                    0x00,
                    0x00, 0x00,
                    0x00,
                    (byte) 0xCE
            };
        }

        private String bytesToHex(byte[] bytes) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02X ", b));
            }
            return sb.toString();
        }
    }

    @PostConstruct
    public void start() {
        NetServer server = vertx.createNetServer();
        server.connectHandler(this::handleNewConnection);
        server.listen(5672, res -> {
            if (res.succeeded()) {
                logger.info("AMQP Broker running on port 5672");
            } else {
                logger.error("Failed to start AMQP broker: {}", res.cause());
            }
        });
    }

    private void handleNewConnection(NetSocket socket) {
        AMQPConnection connection = new AMQPConnection(socket);
        connections.put(socket, connection);

        socket.handler(buffer -> {
            AMQPConnection conn = connections.get(socket);
            if (conn != null) {
                conn.handleIncomingData(buffer);
            }
        });

        socket.closeHandler(v -> {
            connections.remove(socket);
            logger.info("Connection closed with {}", socket.remoteAddress());
        });
    }

    public AMQPServer(Vertx vertx) {
        this.vertx = vertx;
    }
}
