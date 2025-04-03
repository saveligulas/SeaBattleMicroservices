package sg.quarkus.amqp091.protocol.frame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FrameValidationTest {

    @Test
    @DisplayName("Test validating frame end byte")
    public void testValidateFrameEndByte() throws IOException, UnprocessableFrameException {
        // Create a valid method frame
        byte[] frame = FrameBuilder.createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS);
        
        // This should not throw an exception
        IFrame parsedFrame = FrameFactory.parseFrame(frame);
        
        // Now modify the frame end byte to be invalid
        byte[] invalidFrame = frame.clone();
        invalidFrame[invalidFrame.length - 1] = (byte)0xCF; // Wrong end byte
        
        // This should throw an exception
        assertThrows(UnprocessableFrameException.class, () -> {
            FrameFactory.parseFrame(invalidFrame);
        });
    }
    
    @Test
    @DisplayName("Test validating frame length")
    public void testValidateFrameLength() throws IOException {
        // Create a valid method frame
        byte[] frame = FrameBuilder.createMethodFrame((short)1, AMQPPayloadHeaderType.Basic.QOS);
        
        // Now create a truncated frame (too short)
        byte[] shortFrame = new byte[7]; // Just the frame header, no payload or end byte
        System.arraycopy(frame, 0, shortFrame, 0, 7);
        
        // This should throw an exception
        assertThrows(UnprocessableFrameException.class, () -> {
            FrameFactory.parseFrame(shortFrame);
        });
    }
}
