package com.examplatform.service;

import com.examplatform.model.Question;
import com.examplatform.model.Quiz;
import com.examplatform.model.QuizAttempt;
import com.examplatform.repository.QuizRepository;
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
public class AIFeedbackService {
    
    private final QuizRepository quizRepository;
    private final RAGService ragService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    public AIFeedbackService(QuizRepository quizRepository, RAGService ragService) {
        this.quizRepository = quizRepository;
        this.ragService = ragService;
    }
    
    public Map<String, Object> generateDetailedFeedback(QuizAttempt attempt) {
        Quiz quiz = quizRepository.findById(attempt.getQuiz().getId())
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        List<Map<String, Object>> wrongAnswers = analyzeWrongAnswers(quiz, attempt.getAnswers());
        List<String> weakTopics = extractWeakTopics(wrongAnswers);
        String overallFeedback = generateOverallFeedback(attempt, weakTopics);
        
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("attemptId", attempt.getId());
        feedback.put("score", attempt.getScore());
        feedback.put("totalQuestions", attempt.getTotalQuestions());
        feedback.put("percentage", Math.round((attempt.getScore() * 100.0) / attempt.getTotalQuestions()));
        feedback.put("wrongAnswers", wrongAnswers);
        feedback.put("weakTopics", weakTopics);
        feedback.put("overallFeedback", overallFeedback);
        feedback.put("studyRecommendations", generateStudyRecommendations(weakTopics));
        
        return feedback;
    }
    
    private List<Map<String, Object>> analyzeWrongAnswers(Quiz quiz, Map<Long, String> studentAnswers) {
        List<Map<String, Object>> wrongAnswers = new ArrayList<>();
        
        for (Question question : quiz.getQuestions()) {
            String studentAnswer = studentAnswers.get(question.getId());
            String correctAnswer = question.getCorrectAnswer();
            
            if (studentAnswer == null || !studentAnswer.equals(correctAnswer)) {
                Map<String, Object> wrongAnswer = new HashMap<>();
                wrongAnswer.put("questionId", question.getId());
                wrongAnswer.put("questionText", question.getQuestionText());
                wrongAnswer.put("studentAnswer", studentAnswer != null ? studentAnswer : "No answer provided");
                wrongAnswer.put("correctAnswer", correctAnswer);
                wrongAnswer.put("options", question.getOptions());
                wrongAnswer.put("topic", quiz.getTopic());
                
                // Generate AI explanation for this specific wrong answer
                String explanation = generateAnswerExplanation(question, studentAnswer, correctAnswer);
                wrongAnswer.put("explanation", explanation);
                
                wrongAnswers.add(wrongAnswer);
            }
        }
        
        return wrongAnswers;
    }
    
    private String generateAnswerExplanation(Question question, String studentAnswer, String correctAnswer) {
        // First try to get RAG-enhanced explanation
        String ragExplanation = ragService.generateTopicExplanation(
            question.getQuiz().getTopic(), 
            question.getQuestionText()
        );
        
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.trim().isEmpty()) {
            return generateFallbackExplanation(question, studentAnswer, correctAnswer, ragExplanation);
        }
        
        try {
            String prompt = String.format(
                "You are an educational AI tutor. A student answered a question incorrectly. " +
                "Use the knowledge base context to provide a comprehensive explanation.\\n\\n" +
                "QUESTION: %s\\n\\n" +
                "OPTIONS: %s\\n\\n" +
                "STUDENT'S ANSWER: %s\\n" +
                "CORRECT ANSWER: %s\\n\\n" +
                "KNOWLEDGE BASE CONTEXT:\\n%s\\n\\n" +
                "Please provide a detailed explanation that includes:\\n" +
                "1. Why the student's answer is incorrect (if applicable)\\n" +
                "2. Why the correct answer is right\\n" +
                "3. Key concept explanation from the knowledge base\\n" +
                "4. Practical tips to remember this concept\\n" +
                "5. Related concepts the student should review\\n\\n" +
                "Keep the explanation educational and encouraging (max 300 words).",
                question.getQuestionText(),
                String.join(", ", question.getOptions()),
                studentAnswer != null ? studentAnswer : "No answer provided",
                correctAnswer,
                ragExplanation
            );
            
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            ));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
            
        } catch (Exception e) {
            System.err.println("Error generating AI explanation: " + e.getMessage());
        }
        
        return generateFallbackExplanation(question, studentAnswer, correctAnswer, ragExplanation);
    }
    
    private String generateFallbackExplanation(Question question, String studentAnswer, String correctAnswer, String ragContext) {
        StringBuilder explanation = new StringBuilder();
        
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            explanation.append("❌ You didn't provide an answer for this question.\n\n");
        } else {
            explanation.append("❌ Your answer '").append(studentAnswer).append("' is incorrect.\n\n");
        }
        
        explanation.append("✅ The correct answer is '").append(correctAnswer).append("'.\n\n");
        
        if (ragContext != null && !ragContext.trim().isEmpty()) {
            explanation.append("📚 Key Concepts:\n");
            explanation.append(ragContext.length() > 200 ? ragContext.substring(0, 200) + "..." : ragContext);
            explanation.append("\n\n");
        }
        
        explanation.append("💡 Study Tip: Review the fundamental concepts related to ");
        explanation.append(question.getQuiz().getTopic());
        explanation.append(" and practice similar questions to strengthen your understanding.");
        
        return explanation.toString();
    }
    
    private List<String> extractWeakTopics(List<Map<String, Object>> wrongAnswers) {
        return wrongAnswers.stream()
                .map(answer -> (String) answer.get("topic"))
                .distinct()
                .collect(Collectors.toList());
    }
    
    private String generateOverallFeedback(QuizAttempt attempt, List<String> weakTopics) {
        double percentage = (attempt.getScore() * 100.0) / attempt.getTotalQuestions();
        
        StringBuilder feedback = new StringBuilder();
        
        if (percentage >= 80) {
            feedback.append("Excellent work! You scored ").append(Math.round(percentage)).append("%. ");
        } else if (percentage >= 60) {
            feedback.append("Good effort! You scored ").append(Math.round(percentage)).append("%. ");
        } else {
            feedback.append("You scored ").append(Math.round(percentage)).append("%. There's room for improvement. ");
        }
        
        if (!weakTopics.isEmpty()) {
            feedback.append("Focus on improving your understanding of: ")
                    .append(String.join(", ", weakTopics)).append(". ");
        }
        
        feedback.append("Review the detailed explanations below for each incorrect answer.");
        
        return feedback.toString();
    }
    
    private List<Map<String, String>> generateStudyRecommendations(List<String> weakTopics) {
        List<Map<String, String>> recommendations = new ArrayList<>();
        
        for (String topic : weakTopics) {
            Map<String, String> recommendation = new HashMap<>();
            recommendation.put("topic", topic);
            recommendation.put("action", "Review fundamental concepts and practice more questions");
            recommendation.put("resources", "Use the AI tutor to ask specific questions about " + topic);
            recommendations.add(recommendation);
        }
        
        return recommendations;
    }
}