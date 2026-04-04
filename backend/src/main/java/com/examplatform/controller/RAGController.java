package com.examplatform.controller;

import com.examplatform.model.KnowledgeDocument;
import com.examplatform.service.RAGService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "http://localhost:3000")
public class RAGController {
    
    private final RAGService ragService;
    
    public RAGController(RAGService ragService) {
        this.ragService = ragService;
    }
    
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request) {
        String question = (String) request.get("question");
        List<String> topics = (List<String>) request.getOrDefault("topics", List.of());
        Map<String, Object> context = (Map<String, Object>) request.getOrDefault("context", Map.of());
        
        String response = ragService.generateRAGResponse(question, topics, context);
        
        return ResponseEntity.ok(Map.of(
            "question", question,
            "response", response,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeDocument>> searchKnowledge(@RequestParam String query, 
                                                                  @RequestParam(defaultValue = "5") int limit) {
        List<KnowledgeDocument> results = ragService.retrieveRelevantDocuments(query, limit);
        return ResponseEntity.ok(results);
    }
}