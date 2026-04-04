package com.examplatform.controller;

import com.examplatform.service.KnowledgeBaseEnhancementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class KnowledgeBaseController {
    
    private final KnowledgeBaseEnhancementService enhancementService;
    
    public KnowledgeBaseController(KnowledgeBaseEnhancementService enhancementService) {
        this.enhancementService = enhancementService;
    }
    
    @PostMapping("/enhance-python-knowledge")
    public ResponseEntity<Map<String, String>> enhancePythonKnowledge() {
        try {
            enhancementService.addPythonAdvancedConcepts();
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Advanced Python concepts added to knowledge base"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Failed to enhance knowledge base: " + e.getMessage()
            ));
        }
    }
}