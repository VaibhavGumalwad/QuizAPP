package com.examplatform.agent;

import java.util.Map;

public class AgentRequest {
    private String query;
    private Map<String, Object> studentData;
    private String userId;
    
    public AgentRequest() {}
    
    public AgentRequest(String query, Map<String, Object> studentData, String userId) {
        this.query = query;
        this.studentData = studentData;
        this.userId = userId;
    }
    
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public Map<String, Object> getStudentData() { return studentData; }
    public void setStudentData(Map<String, Object> studentData) { this.studentData = studentData; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}