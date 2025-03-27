package sg.quarkus.amqp091;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import jakarta.inject.Singleton;
import sg.quarkus.amqp091.protocol.frame.Header;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Singleton
class AmqpVerticle extends AbstractVerticle {
    static final int PORT = 5672;

    @Override
    public void start() throws Exception {
        NetServer server = vertx.createNetServer();
        server.connectHandler(this::handleConnect);
        server.listen(PORT, res -> {
            if (res.succeeded()) {
                System.out.println("AMQP Broker running on port " + PORT);
            } else {
                System.err.println("Failed to start AMQP broker: " + res.cause());
            }
        });
    }

    private void handleConnect(NetSocket socket) {
        socket.handler(buffer -> {
            String received = buffer.toString();
            System.out.println("Received (string): " + received);
            System.out.println("Received (raw bytes): " + Arrays.toString(received.getBytes(StandardCharsets.US_ASCII)));
            System.out.println("Received (hex): " + bytesToHex(received.getBytes(StandardCharsets.US_ASCII)));

            byte[] response;

            if (!Arrays.equals(received.getBytes(), Header.PROTOCOL_HEADER)) {
                response = new byte[] {
                        (byte) 0x00, (byte) 0x01,   // channel_max (1 channel)
                        (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,   // frame_max (256 bytes)
                        (byte) 0x00, (byte) 0x01    // heartbeat (1 second)
                };

            } else {
                response = new byte[]{
                        // Frame Header
                        0x01,             // Type: 0x01 (Method Frame)
                        0x00, 0x00,       // Channel: 0x0000 (Channel 0)
                        0x00, 0x00, 0x00, 0x2E, // Size: 0x0000002E (example size of payload)

                        // Method Frame Payload (Connection.Start)
                        0x00, 0x0A,       // Class ID: 0x000A (Connection class)
                        0x00, 0x0A,       // Method ID: 0x000A (Connection.Start method)

                        // Arguments:
                        0x09,             // Version Major: 0x09
                        0x01,             // Version Minor: 0x01

                        // Server Properties: Empty field table (0x00 for simplicity)
                        0x00, 0x00,       // Empty Field Table (0 length)

                        // Mech List: "PLAIN" (Short String)
                        0x05,             // Length of the string "PLAIN"
                        'P', 'L', 'A', 'I', 'N', // String "PLAIN"

                        // Locales: "en_US" (Short String)
                        0x05,             // Length of the string "en_US"
                        'e', 'n', '_', 'U', 'S', // String "en_US"

                        // Frame End
                        (byte) 0xCE       // Frame End: 0xCE
                };
            }


            socket.write(io.vertx.core.buffer.Buffer.buffer(response));
            System.out.println("Sent Connection.Start frame.");
        });
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

}
