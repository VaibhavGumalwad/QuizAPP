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

        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.trim().isEmpty()) {
            throw new RuntimeException("Gemini API key is not configured.");
        }

        try {
            String prompt = String.format(
                "Generate %d multiple choice questions about '%s'. " +
                "Each question must have EXACTLY 4 options labeled as 'Option A', 'Option B', 'Option C', and 'Option D'. " +
                "One of these options must be the correct answer. " +
                "Return ONLY a valid JSON array with this EXACT structure (no markdown, no code blocks, no explanation): " +
                "[{\"question\":\"What is the capital of France?\",\"options\":[\"Option A: Paris\",\"Option B: London\",\"Option C: Berlin\",\"Option D: Madrid\"],\"correctAnswer\":\"Option A: Paris\"}]. " +
                "Make sure each option starts with 'Option A:', 'Option B:', 'Option C:', or 'Option D:' followed by the answer text. " +
                "Generate exactly %d questions following this exact format.",
                numberOfQuestions, topic, numberOfQuestions);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 4096
                )
            ));

            System.out.println("Calling Gemini API at: " + apiUrl);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Gemini API Response Status: " + response.statusCode());
            System.out.println("Gemini API Raw Response: " + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Gemini API returned status " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.has("candidates") || root.path("candidates").isEmpty()) {
                throw new RuntimeException("Gemini response missing candidates: " + response.body());
            }

            JsonNode candidate = root.path("candidates").get(0);
            String finishReason = candidate.path("finishReason").asText("");
            if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
                throw new RuntimeException("Gemini blocked the response. Reason: " + finishReason);
            }

            // gemini-2.5-flash returns multiple parts (thinking + text), find the text part
            String content = "";
            JsonNode parts = candidate.path("content").path("parts");
            for (JsonNode part : parts) {
                if (!part.has("thought") || !part.path("thought").asBoolean()) {
                    content = part.path("text").asText();
                    break;
                }
            }
            if (content.isEmpty()) {
                content = parts.get(0).path("text").asText();
            }
            System.out.println("Gemini generated content: " + content);

            // Strip markdown code fences if present
            content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();

            // Extract JSON array if there's surrounding text
            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start != -1 && end != -1 && end > start) {
                content = content.substring(start, end + 1);
            }

            List<Map<String, Object>> questions = objectMapper.readValue(content, List.class);
            System.out.println("Successfully generated " + questions.size() + " questions with Gemini AI");
            return questions;

        } catch (Exception e) {
            System.err.println("Error generating quiz with Gemini: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate quiz using Gemini AI: " + e.getMessage(), e);
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
