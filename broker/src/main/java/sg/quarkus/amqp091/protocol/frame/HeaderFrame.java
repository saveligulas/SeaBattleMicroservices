package sg.quarkus.amqp091.protocol.frame;

import sg.quarkus.amqp091.protocol.frame.error.UnprocessableFrameException;

import java.util.HashMap;
import java.util.Map;

public class HeaderFrame extends AbstractFrame {
    private final short classId;
    private final long bodySize;
    private final Map<String, Object> properties;
    
    public HeaderFrame(byte[] data) throws UnprocessableFrameException {
        super(data);
        checkLength();
        checkEndByte();
        
        // Class ID is at offset 7 (after frame header)
        this.classId = NumberUtils.toShort(data[7], data[8]);
        
        // Body size is at offset 11 (after class ID and weight)
        this.bodySize = NumberUtils.toLong(
            data[11], data[12], data[13], data[14], 
            data[15], data[16], data[17], data[18]
        );
        
        // Parse properties
        this.properties = parseProperties(data, 19);
    }
    
    private Map<String, Object> parseProperties(byte[] data, int offset) {
        // In a complete implementation, you would parse the property flags and values
        // This is a simplified version that returns an empty map
        return new HashMap<>();
    }
    
    public short getClassId() {
        return classId;
    }
    
    public long getBodySize() {
        return bodySize;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    @Override
    public byte[] getData() {
        return data;
    }
}
