package sg.quarkus.amqp091.broker;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Message {
    private final String id;
    private final byte[] body;
    private final Map<String, Object> headers;
    private final Map<String, Object> properties;
    private final String exchange;
    private final String routingKey;
    private final String connectionId;
    
    private Message(byte[] body, Map<String, Object> headers, Map<String, Object> properties, 
                  String exchange, String routingKey, String connectionId) {
        this.id = UUID.randomUUID().toString();
        this.body = body;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.connectionId = connectionId;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public String getId() {
        return id;
    }
    
    public byte[] getBody() {
        return body;
    }
    
    public String getBodyAsString() {
        return new String(body, StandardCharsets.UTF_8);
    }
    
    public Map<String, Object> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
    
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
    
    public String getExchange() {
        return exchange;
    }
    
    public String getRoutingKey() {
        return routingKey;
    }
    
    public String getConnectionId() {
        return connectionId;
    }
    
    public boolean isFromSameConnection(String connectionId) {
        return this.connectionId != null && this.connectionId.equals(connectionId);
    }
    
    public boolean isFromSameConnection() {
        return connectionId != null;
    }
    
    public int getSize() {
        return body != null ? body.length : 0;
    }
    
    public static class Builder {
        private byte[] body;
        private Map<String, Object> headers = new HashMap<>();
        private Map<String, Object> properties = new HashMap<>();
        private String exchange;
        private String routingKey;
        private String connectionId;
        
        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }
        
        public Builder bodyString(String content) {
            this.body = content != null ? content.getBytes(StandardCharsets.UTF_8) : null;
            return this;
        }
        
        public Builder header(String key, Object value) {
            this.headers.put(key, value);
            return this;
        }
        
        public Builder headers(Map<String, Object> headers) {
            if (headers != null) {
                this.headers.putAll(headers);
            }
            return this;
        }
        
        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }
        
        public Builder properties(Map<String, Object> properties) {
            if (properties != null) {
                this.properties.putAll(properties);
            }
            return this;
        }
        
        public Builder exchange(String exchange) {
            this.exchange = exchange;
            return this;
        }
        
        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }
        
        public Builder connectionId(String connectionId) {
            this.connectionId = connectionId;
            return this;
        }
        
        public Message build() {
            return new Message(body, headers, properties, exchange, routingKey, connectionId);
        }
    }
}