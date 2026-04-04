package com.examplatform.agent;

import java.util.Map;

public class AgentResponse {
    private String message;
    private Map<String, Object> data;
    private long timestamp;
    
    public AgentResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public AgentResponse(String message, Map<String, Object> data) {
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}