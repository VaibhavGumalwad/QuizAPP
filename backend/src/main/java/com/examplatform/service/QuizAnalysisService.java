package com.examplatform.service;

import com.examplatform.model.Question;
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
public class QuizAnalysisService {
    
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
    
    public QuizAnalysisService(QuizAttemptRepository attemptRepository, RAGService ragService) {
        this.attemptRepository = attemptRepository;
        this.ragService = ragService;
    }
    
    public Map<String, Object> analyzeAttempt(Long attemptId) {
        Optional<QuizAttempt> attemptOpt = attemptRepository.findById(attemptId);
        
        if (attemptOpt.isEmpty()) {
            return Map.of("error", "Attempt not found");
        }
        
        QuizAttempt attempt = attemptOpt.get();
        Quiz quiz = attempt.getQuiz();
        Map<Long, String> studentAnswers = attempt.getAnswers();
        
        List<Map<String, Object>> questionAnalysis = new ArrayList<>();
        int correctCount = 0;
        List<String> incorrectQuestions = new ArrayList<>();
        
        for (Question question : quiz.getQuestions()) {
            String studentAnswer = studentAnswers.get(question.getId());
            String correctAnswer = question.getCorrectAnswer();
            boolean isCorrect = correctAnswer.equals(studentAnswer);
            
            if (isCorrect) {
                correctCount++;
            } else {
                incorrectQuestions.add(question.getQuestionText());
            }
            
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("questionId", question.getId());
            analysis.put("question", question.getQuestionText());
            analysis.put("yourAnswer", studentAnswer != null ? studentAnswer : "Not answered");
            analysis.put("correctAnswer", correctAnswer);
            analysis.put("isCorrect", isCorrect);
            analysis.put("options", question.getOptions());
            
            questionAnalysis.add(analysis);
        }
        
        double percentage = (correctCount * 100.0) / quiz.getQuestions().size();
        String aiExplanation = generateRAGExplanation(quiz.getTopic(), incorrectQuestions, percentage, attempt);
        
        Map<String, Object> result = new HashMap<>();
        result.put("attemptId", attemptId);
        result.put("quizTitle", quiz.getTitle());
        result.put("topic", quiz.getTopic());
        result.put("score", correctCount);
        result.put("totalQuestions", quiz.getQuestions().size());
        result.put("percentage", Math.round(percentage * 100.0) / 100.0);
        result.put("attemptedAt", attempt.getAttemptedAt());
        result.put("questionAnalysis", questionAnalysis);
        result.put("aiExplanation", aiExplanation);
        
        return result;
    }
    
    private String generateRAGExplanation(String topic, List<String> incorrectQuestions, double percentage, QuizAttempt attempt) {
        if (incorrectQuestions.isEmpty()) {
            return "Perfect score! You have mastered this topic. Keep up the excellent work!";
        }
        
        Map<String, Object> context = new HashMap<>();
        context.put("topic", topic);
        context.put("score", percentage);
        context.put("incorrectCount", incorrectQuestions.size());
        context.put("studentId", attempt.getStudent().getId());
        
        String query = String.format(
            "I scored %.1f%% on a %s quiz and got %d questions wrong. How can I improve my understanding?",
            percentage, topic, incorrectQuestions.size()
        );
        
        return ragService.generateRAGResponse(query, Arrays.asList(topic), context);
    }
}
