package sg.quarkus.amqp091.protocol.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import sg.quarkus.amqp091.protocol.frame.data.AMQPDataField;
import sg.quarkus.amqp091.protocol.frame.data.AMQPDataType;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FrameParserTest {

    @Test
    @DisplayName("Test parsing protocol header")
    public void testParseProtocolHeader() {
        assertDoesNotThrow(() -> {
            // Test correct protocol header
            byte[] protocolHeader = StandardFrames.PROTOCOL_HEADER;
            IFrame frame = FrameFactory.parseProtocolHeader(protocolHeader);
            assertTrue(frame instanceof DiscoveryFrame);
        });
        
        assertThrows(UnprocessableFrameException.class, () -> {
            // Test invalid protocol header length
            byte[] invalidHeader = new byte[]{0x41, 0x4D, 0x51, 0x50, 0x00, 0x00, 0x09}; // Missing last byte
            FrameFactory.parseProtocolHeader(invalidHeader);
        });
        
        assertThrows(UnprocessableFrameException.class, () -> {
            // Test invalid protocol header identifier
            byte[] invalidHeader = new byte[]{0x42, 0x4D, 0x51, 0x50, 0x00, 0x00, 0x09, 0x01}; // Starts with 'B' instead of 'A'
            FrameFactory.parseProtocolHeader(invalidHeader);
        });
    }
    
    @Test
    @DisplayName("Test parsing method frames")
    public void testParseMethodFrame() {
        assertDoesNotThrow(() -> {
            // Test parsing a connection start frame
            byte[] connectionStartFrame = StandardFrames.CONNECTION_START;
            IFrame frame = FrameFactory.parseFrame(connectionStartFrame);
            
            assertTrue(frame instanceof MethodFrame);
            MethodFrame methodFrame = (MethodFrame) frame;
            assertEquals(AMQPPayloadHeaderType.Connection.START.getClassId(), methodFrame.getClassId());
            assertEquals(AMQPPayloadHeaderType.Connection.START.getMethodId(), methodFrame.getMethodId());
        });
    }
    
    @Test
    @DisplayName("Test parsing header frames")
    public void testParseHeaderFrame() throws IOException {
        // Create a header frame
        byte[] headerFrame = StandardFrames.createContentHeaderFrame((short)1, (short)60, 100L); // Basic class, body size 100
        
        assertDoesNotThrow(() -> {
            IFrame frame = FrameFactory.parseFrame(headerFrame);
            assertTrue(frame instanceof HeaderFrame);
            HeaderFrame contentHeader = (HeaderFrame) frame;
            assertEquals(60, contentHeader.getClassId()); // Basic class
            assertEquals(100L, contentHeader.getBodySize());
        });
    }
    
    @Test
    @DisplayName("Test parsing body frames")
    public void testParseBodyFrame() throws IOException {
        // Test content body frame
        byte[] content = "Hello AMQP!".getBytes();
        byte[] bodyFrame = StandardFrames.createContentBodyFrame((short)1, content);
        
        assertDoesNotThrow(() -> {
            IFrame frame = FrameFactory.parseFrame(bodyFrame);
            assertTrue(frame instanceof BodyFrame);
        });
    }
    
    @Test
    @DisplayName("Test parsing heartbeat frames")
    public void testParseHeartbeatFrame() {
        assertDoesNotThrow(() -> {
            byte[] heartbeatFrame = StandardFrames.HEARTBEAT;
            IFrame frame = FrameFactory.parseFrame(heartbeatFrame);
            assertTrue(frame instanceof HeartbeatFrame);
        });
    }
    
    @Test
    @DisplayName("Test building method frames with data fields")
    public void testBuildMethodFrameWithFields() throws IOException, UnprocessableFrameException {
        // Create a method frame with fields
        List<AMQPDataField> fields = new ArrayList<>();
        fields.add(new AMQPDataField("queue", "test-queue", AMQPDataType.SHORT_STRING));
        fields.add(new AMQPDataField("passive", false, AMQPDataType.BIT));
        fields.add(new AMQPDataField("durable", true, AMQPDataType.BIT));
        fields.add(new AMQPDataField("exclusive", false, AMQPDataType.BIT));
        fields.add(new AMQPDataField("auto-delete", true, AMQPDataType.BIT));
        fields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT));
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl", 60000);
        fields.add(new AMQPDataField("arguments", arguments, AMQPDataType.FIELD_TABLE));
        
        // Use the new FrameBuilder with generics to create the frame
        byte[] queueDeclareFrame = FrameBuilder.createMethodFrame(
            (short)1, 
            AMQPPayloadHeaderType.Queue.DECLARE, 
            fields
        );
        
        // Now parse it back
        IFrame frame = FrameFactory.parseFrame(queueDeclareFrame);
        assertTrue(frame instanceof MethodFrame);
        MethodFrame methodFrame = (MethodFrame) frame;
        assertEquals(AMQPPayloadHeaderType.Queue.DECLARE.getClassId(), methodFrame.getClassId());
        assertEquals(AMQPPayloadHeaderType.Queue.DECLARE.getMethodId(), methodFrame.getMethodId());
    }
    
    @Test
    @DisplayName("Test frame with invalid type")
    public void testInvalidFrameType() {
        byte[] invalidFrame = new byte[] {
            5, // Invalid frame type (not 1, 2, 3, or 8)
            0, 1, // Channel 1
            0, 0, 0, 4, // Payload size 4
            0, 0, 0, 0, // Dummy payload
            (byte)0xCE // Frame end
        };
        
        assertThrows(UnprocessableFrameException.class, () -> {
            FrameFactory.parseFrame(invalidFrame);
        });
    }
    
    @Test
    @DisplayName("Test frame with invalid end marker")
    public void testInvalidFrameEnd() {
        byte[] invalidFrame = new byte[] {
            1, // Method frame type
            0, 1, // Channel 1
            0, 0, 0, 4, // Payload size 4
            0, 0, 0, 0, // Dummy payload
            (byte)0xCF // Wrong frame end marker
        };
        
        assertThrows(UnprocessableFrameException.class, () -> {
            FrameFactory.parseFrame(invalidFrame);
        });
    }
    
    @Test
    @DisplayName("Test frame too short to be valid")
    public void testFrameTooShort() {
        byte[] shortFrame = new byte[] {
            1, // Method frame type
            0, 1 // Channel 1 - missing the rest of the frame
        };
        
        assertThrows(UnprocessableFrameException.class, () -> {
            FrameFactory.parseFrame(shortFrame);
        });
    }
    
    @Test
    @DisplayName("Test complete connection startup sequence")
    public void testConnectionStartupSequence() throws IOException, UnprocessableFrameException {
        // Test protocol header
        byte[] protocolHeader = StandardFrames.PROTOCOL_HEADER;
        assertDoesNotThrow(() -> FrameFactory.parseProtocolHeader(protocolHeader));
        
        // Test connection start
        byte[] connectionStart = StandardFrames.CONNECTION_START;
        IFrame startFrame = FrameFactory.parseFrame(connectionStart);
        assertTrue(startFrame instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.START.getClassId(), ((MethodFrame)startFrame).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.START.getMethodId(), ((MethodFrame)startFrame).getMethodId());
        
        // Create a connection start-ok with client properties
        Map<String, Object> clientProperties = new HashMap<>();
        clientProperties.put("product", "AMQP Test Client");
        clientProperties.put("version", "1.0");
        clientProperties.put("platform", "Java");
        
        List<AMQPDataField> startOkFields = new ArrayList<>();
        startOkFields.add(new AMQPDataField("client-properties", clientProperties, AMQPDataType.FIELD_TABLE));
        startOkFields.add(new AMQPDataField("mechanism", "PLAIN", AMQPDataType.SHORT_STRING));
        startOkFields.add(new AMQPDataField("response", "\0guest\0guest", AMQPDataType.LONG_STRING));
        startOkFields.add(new AMQPDataField("locale", "en_US", AMQPDataType.SHORT_STRING));
        
        byte[] startOkFrame = FrameBuilder.createMethodFrame(
            (short)0, 
            AMQPPayloadHeaderType.Connection.START_OK,
            startOkFields
        );
        
        IFrame parsedStartOk = FrameFactory.parseFrame(startOkFrame);
        assertTrue(parsedStartOk instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.START_OK.getClassId(), ((MethodFrame)parsedStartOk).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.START_OK.getMethodId(), ((MethodFrame)parsedStartOk).getMethodId());
        
        // Test connection tune
        byte[] connectionTune = StandardFrames.CONNECTION_TUNE;
        IFrame tuneFrame = FrameFactory.parseFrame(connectionTune);
        assertTrue(tuneFrame instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.TUNE.getClassId(), ((MethodFrame)tuneFrame).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.TUNE.getMethodId(), ((MethodFrame)tuneFrame).getMethodId());
        
        // Create a tune-ok frame
        List<AMQPDataField> tuneOkFields = new ArrayList<>();
        tuneOkFields.add(new AMQPDataField("channel-max", (short)2047, AMQPDataType.SHORT));
        tuneOkFields.add(new AMQPDataField("frame-max", 131072, AMQPDataType.INT));
        tuneOkFields.add(new AMQPDataField("heartbeat", (short)60, AMQPDataType.SHORT));
        
        byte[] tuneOkFrame = FrameBuilder.createMethodFrame(
            (short)0, 
            AMQPPayloadHeaderType.Connection.TUNE_OK,
            tuneOkFields
        );
        
        IFrame parsedTuneOk = FrameFactory.parseFrame(tuneOkFrame);
        assertTrue(parsedTuneOk instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.TUNE_OK.getClassId(), ((MethodFrame)parsedTuneOk).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.TUNE_OK.getMethodId(), ((MethodFrame)parsedTuneOk).getMethodId());
        
        // Create a connection open frame
        List<AMQPDataField> openFields = new ArrayList<>();
        openFields.add(new AMQPDataField("virtual-host", "/", AMQPDataType.SHORT_STRING));
        openFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING));
        openFields.add(new AMQPDataField("reserved-2", false, AMQPDataType.BIT));
        
        byte[] openFrame = FrameBuilder.createMethodFrame(
            (short)0, 
            AMQPPayloadHeaderType.Connection.OPEN,
            openFields
        );
        
        IFrame parsedOpen = FrameFactory.parseFrame(openFrame);
        assertTrue(parsedOpen instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.OPEN.getClassId(), ((MethodFrame)parsedOpen).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.OPEN.getMethodId(), ((MethodFrame)parsedOpen).getMethodId());
        
        // Create a connection open-ok frame
        List<AMQPDataField> openOkFields = new ArrayList<>();
        openOkFields.add(new AMQPDataField("reserved-1", "", AMQPDataType.SHORT_STRING));
        
        byte[] openOkFrame = FrameBuilder.createMethodFrame(
            (short)0, 
            AMQPPayloadHeaderType.Connection.OPEN_OK,
            openOkFields
        );
        
        IFrame parsedOpenOk = FrameFactory.parseFrame(openOkFrame);
        assertTrue(parsedOpenOk instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Connection.OPEN_OK.getClassId(), ((MethodFrame)parsedOpenOk).getClassId());
        assertEquals(AMQPPayloadHeaderType.Connection.OPEN_OK.getMethodId(), ((MethodFrame)parsedOpenOk).getMethodId());
    }
    
    @Test
    @DisplayName("Test basic publish and delivery sequence")
    public void testBasicPublishAndDelivery() throws IOException, UnprocessableFrameException {
        // Create a basic publish frame
        List<AMQPDataField> publishFields = new ArrayList<>();
        publishFields.add(new AMQPDataField("exchange", "amq.direct", AMQPDataType.SHORT_STRING));
        publishFields.add(new AMQPDataField("routing-key", "test-queue", AMQPDataType.SHORT_STRING));
        publishFields.add(new AMQPDataField("mandatory", false, AMQPDataType.BIT));
        publishFields.add(new AMQPDataField("immediate", false, AMQPDataType.BIT));
        
        byte[] publishFrame = FrameBuilder.createMethodFrame(
            (short)1, 
            AMQPPayloadHeaderType.Basic.PUBLISH,
            publishFields
        );
        
        IFrame parsedPublish = FrameFactory.parseFrame(publishFrame);
        assertTrue(parsedPublish instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Basic.PUBLISH.getClassId(), ((MethodFrame)parsedPublish).getClassId());
        assertEquals(AMQPPayloadHeaderType.Basic.PUBLISH.getMethodId(), ((MethodFrame)parsedPublish).getMethodId());
        
        // Create content header frame
        byte[] headerFrame = FrameBuilder.createContentHeaderFrame((short)1, (short)60, 11);
        
        IFrame parsedHeader = FrameFactory.parseFrame(headerFrame);
        assertTrue(parsedHeader instanceof HeaderFrame);
        assertEquals(60, ((HeaderFrame)parsedHeader).getClassId());
        assertEquals(11, ((HeaderFrame)parsedHeader).getBodySize());
        
        // Create content body frame
        byte[] content = "Hello World".getBytes();
        byte[] bodyFrame = FrameBuilder.createContentBodyFrame((short)1, content);
        
        IFrame parsedBody = FrameFactory.parseFrame(bodyFrame);
        assertTrue(parsedBody instanceof BodyFrame);
        
        // Create a basic deliver frame (server -> client)
        List<AMQPDataField> deliverFields = new ArrayList<>();
        deliverFields.add(new AMQPDataField("consumer-tag", "consumer-123", AMQPDataType.SHORT_STRING));
        deliverFields.add(new AMQPDataField("delivery-tag", 1L, AMQPDataType.LONGLONG));
        deliverFields.add(new AMQPDataField("redelivered", false, AMQPDataType.BIT));
        deliverFields.add(new AMQPDataField("exchange", "amq.direct", AMQPDataType.SHORT_STRING));
        deliverFields.add(new AMQPDataField("routing-key", "test-queue", AMQPDataType.SHORT_STRING));
        
        byte[] deliverFrame = FrameBuilder.createMethodFrame(
            (short)1, 
            AMQPPayloadHeaderType.Basic.DELIVER,
            deliverFields
        );
        
        IFrame parsedDeliver = FrameFactory.parseFrame(deliverFrame);
        assertTrue(parsedDeliver instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Basic.DELIVER.getClassId(), ((MethodFrame)parsedDeliver).getClassId());
        assertEquals(AMQPPayloadHeaderType.Basic.DELIVER.getMethodId(), ((MethodFrame)parsedDeliver).getMethodId());
    }
    
    @Test
    @DisplayName("Test queue operations")
    public void testQueueOperations() throws IOException, UnprocessableFrameException {
        // Test queue declare
        List<AMQPDataField> declareFields = new ArrayList<>();
        declareFields.add(new AMQPDataField("queue", "test-queue", AMQPDataType.SHORT_STRING));
        declareFields.add(new AMQPDataField("passive", false, AMQPDataType.BIT));
        declareFields.add(new AMQPDataField("durable", true, AMQPDataType.BIT));
        declareFields.add(new AMQPDataField("exclusive", false, AMQPDataType.BIT));
        declareFields.add(new AMQPDataField("auto-delete", false, AMQPDataType.BIT));
        declareFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT));
        
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl", 60000);
        declareFields.add(new AMQPDataField("arguments", arguments, AMQPDataType.FIELD_TABLE));
        
        byte[] declareFrame = FrameBuilder.createMethodFrame(
            (short)1, 
            AMQPPayloadHeaderType.Queue.DECLARE,
            declareFields
        );
        
        IFrame parsedDeclare = FrameFactory.parseFrame(declareFrame);
        assertTrue(parsedDeclare instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Queue.DECLARE.getClassId(), ((MethodFrame)parsedDeclare).getClassId());
        assertEquals(AMQPPayloadHeaderType.Queue.DECLARE.getMethodId(), ((MethodFrame)parsedDeclare).getMethodId());
        
        // Test queue bind
        List<AMQPDataField> bindFields = new ArrayList<>();
        bindFields.add(new AMQPDataField("queue", "test-queue", AMQPDataType.SHORT_STRING));
        bindFields.add(new AMQPDataField("exchange", "amq.direct", AMQPDataType.SHORT_STRING));
        bindFields.add(new AMQPDataField("routing-key", "test-key", AMQPDataType.SHORT_STRING));
        bindFields.add(new AMQPDataField("no-wait", false, AMQPDataType.BIT));
        
        Map<String, Object> bindArguments = new HashMap<>();
        bindFields.add(new AMQPDataField("arguments", bindArguments, AMQPDataType.FIELD_TABLE));
        
        byte[] bindFrame = FrameBuilder.createMethodFrame(
            (short)1, 
            AMQPPayloadHeaderType.Queue.BIND,
            bindFields
        );
        
        IFrame parsedBind = FrameFactory.parseFrame(bindFrame);
        assertTrue(parsedBind instanceof MethodFrame);
        assertEquals(AMQPPayloadHeaderType.Queue.BIND.getClassId(), ((MethodFrame)parsedBind).getClassId());
        assertEquals(AMQPPayloadHeaderType.Queue.BIND.getMethodId(), ((MethodFrame)parsedBind).getMethodId());
    }
}
