package com.examplatform.controller;

import com.examplatform.agent.AgentRequest;
import com.examplatform.agent.AgentResponse;
import com.examplatform.agent.EducationalAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "http://localhost:3000")
public class AgentController {
    
    private final EducationalAgent educationalAgent;
    
    public AgentController(EducationalAgent educationalAgent) {
        this.educationalAgent = educationalAgent;
    }
    
    @PostMapping("/process")
    public ResponseEntity<AgentResponse> processRequest(@RequestBody Map<String, Object> requestData) {
        String query = (String) requestData.get("query");
        Map<String, Object> studentData = (Map<String, Object>) requestData.getOrDefault("studentData", Map.of());
        String userId = (String) requestData.getOrDefault("userId", "anonymous");
        
        AgentRequest request = new AgentRequest(query, studentData, userId);
        AgentResponse response = educationalAgent.processRequest(request);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/analyze-performance")
    public ResponseEntity<AgentResponse> analyzePerformance(@RequestBody Map<String, Object> studentData) {
        AgentRequest request = new AgentRequest("analyze my performance", studentData, "student");
        AgentResponse response = educationalAgent.processRequest(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/generate-study-plan")
    public ResponseEntity<AgentResponse> generateStudyPlan(@RequestBody Map<String, Object> studentData) {
        AgentRequest request = new AgentRequest("generate study plan", studentData, "student");
        AgentResponse response = educationalAgent.processRequest(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/create-assessment")
    public ResponseEntity<AgentResponse> createAssessment(@RequestBody Map<String, Object> requestData) {
        String topic = (String) requestData.get("topic");
        String query = "create assessment on " + topic;
        AgentRequest request = new AgentRequest(query, Map.of(), "teacher");
        AgentResponse response = educationalAgent.processRequest(request);
        return ResponseEntity.ok(response);
    }
}