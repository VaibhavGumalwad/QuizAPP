package com.examplatform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AIQuizGeneratorService {
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    public List<Map<String, Object>> generateQuiz(String topic, int numberOfQuestions) {
        System.out.println("Starting quiz generation for topic: " + topic + ", questions: " + numberOfQuestions);
        
        // Check if API key is configured
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.trim().isEmpty()) {
            System.out.println("Gemini API key not configured. Using mock quiz.");
            return generateMockQuiz(topic, numberOfQuestions);
        }
        
        try {
            String prompt = String.format(
                "Generate %d multiple choice questions about '%s'. " +
                "Each question must have EXACTLY 4 options labeled as 'Option A', 'Option B', 'Option C', and 'Option D'. " +
                "One of these options must be the correct answer. " +
                "Return ONLY a valid JSON array with this EXACT structure (no markdown, no code blocks, no explanation): " +
                "[{\"question\":\"What is the capital of France?\",\"options\":[\"Option A: Paris\",\"Option B: London\",\"Option C: Berlin\",\"Option D: Madrid\"],\"correctAnswer\":\"Option A: Paris\"}]. " +
                "Make sure each option starts with 'Option A:', 'Option B:', 'Option C:', or 'Option D:' followed by the answer text. " +
                "Generate %d questions following this exact format.", 
                numberOfQuestions, topic, numberOfQuestions);
            
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            ));
            
            System.out.println("Calling Gemini API...");
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("Gemini API Response Status: " + response.statusCode());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                
                // Check if response has the expected structure
                if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                    System.err.println("Invalid Gemini response structure: " + response.body());
                    return generateMockQuiz(topic, numberOfQuestions);
                }
                
                String content = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();
                
                System.out.println("Gemini generated content: " + content);
                
                // Clean the response - remove markdown code blocks if present
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                
                List<Map<String, Object>> questions = objectMapper.readValue(content, List.class);
                System.out.println("Successfully generated " + questions.size() + " questions with Gemini AI");
                return questions;
            } else {
                System.err.println("Gemini API Error (" + response.statusCode() + "): " + response.body());
                return generateMockQuiz(topic, numberOfQuestions);
            }
        } catch (Exception e) {
            System.err.println("Error generating quiz with Gemini: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return generateMockQuiz(topic, numberOfQuestions);
        }
    }
    
    private List<Map<String, Object>> generateMockQuiz(String topic, int numberOfQuestions) {
        System.out.println("Generating mock quiz for: " + topic);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 1; i <= numberOfQuestions; i++) {
            questions.add(Map.of(
                "question", "Sample question " + i + " about " + topic + "?",
                "options", List.of(
                    "Option A: First answer", 
                    "Option B: Second answer", 
                    "Option C: Third answer", 
                    "Option D: Fourth answer"
                ),
                "correctAnswer", "Option A: First answer"
            ));
        }
        return questions;
    }
}
