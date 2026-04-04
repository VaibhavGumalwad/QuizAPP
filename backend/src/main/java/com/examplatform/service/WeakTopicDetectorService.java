package com.examplatform.service;

import com.examplatform.model.Quiz;
import com.examplatform.model.QuizAttempt;
import com.examplatform.repository.QuizAttemptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WeakTopicDetectorService {
    
    private final QuizAttemptRepository attemptRepository;
    private final RAGService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    public WeakTopicDetectorService(QuizAttemptRepository attemptRepository, RAGService ragService) {
        this.attemptRepository = attemptRepository;
        this.ragService = ragService;
    }
    
    public Map<String, Object> analyzeWeakTopics(Long studentId) {
        List<QuizAttempt> attempts = attemptRepository.findByStudentId(studentId);
        
        if (attempts.isEmpty()) {
            return Map.of("message", "No quiz attempts found", "weakTopics", List.of());
        }
        
        Map<String, List<Double>> topicScores = new HashMap<>();
        
        for (QuizAttempt attempt : attempts) {
            Quiz quiz = attempt.getQuiz();
            String topic = quiz.getTopic();
            double scorePercentage = (attempt.getScore() * 100.0) / attempt.getTotalQuestions();
            
            topicScores.computeIfAbsent(topic, k -> new ArrayList<>()).add(scorePercentage);
        }
        
        List<Map<String, Object>> weakTopics = topicScores.entrySet().stream()
                .map(entry -> {
                    String topic = entry.getKey();
                    List<Double> scores = entry.getValue();
                    double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                    
                    Map<String, Object> topicMap = new HashMap<>();
                    topicMap.put("topic", topic);
                    topicMap.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
                    topicMap.put("attempts", scores.size());
                    topicMap.put("isWeak", avgScore < 60);
                    return topicMap;
                })
                .sorted((a, b) -> Double.compare((Double) a.get("averageScore"), (Double) b.get("averageScore")))
                .collect(Collectors.toList());
        
        String aiRecommendations = generateRAGRecommendations(weakTopics);
        
        return Map.of(
                "weakTopics", weakTopics,
                "recommendations", aiRecommendations,
                "totalAttempts", attempts.size()
        );
    }
    
    private String generateRAGRecommendations(List<Map<String, Object>> weakTopics) {
        List<String> weakTopicNames = weakTopics.stream()
                .filter(t -> (Boolean) t.get("isWeak"))
                .map(t -> (String) t.get("topic"))
                .collect(Collectors.toList());
        
        if (weakTopicNames.isEmpty()) {
            return "Great job! You're performing well across all topics. Keep up the excellent work!";
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("weakTopics", weakTopicNames);
        context.put("performanceData", weakTopics);
        
        String query = "How can I improve my understanding of " + String.join(", ", weakTopicNames) + "?";
        
        return ragService.generateRAGResponse(query, weakTopicNames, context);
    }
}
