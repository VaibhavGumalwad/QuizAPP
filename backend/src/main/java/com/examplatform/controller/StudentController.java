package com.examplatform.controller;

import com.examplatform.dto.SubmitQuizRequest;
import com.examplatform.model.Quiz;
import com.examplatform.model.QuizAttempt;
import com.examplatform.service.QuizAttemptService;
import com.examplatform.service.QuizService;
import com.examplatform.service.WeakTopicDetectorService;
import com.examplatform.service.QuizAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;
    private final WeakTopicDetectorService weakTopicDetectorService;
    private final QuizAnalysisService quizAnalysisService;
    
    public StudentController(QuizService quizService, QuizAttemptService quizAttemptService, 
                           WeakTopicDetectorService weakTopicDetectorService, QuizAnalysisService quizAnalysisService) {
        this.quizService = quizService;
        this.quizAttemptService = quizAttemptService;
        this.weakTopicDetectorService = weakTopicDetectorService;
        this.quizAnalysisService = quizAnalysisService;
    }
    
    @GetMapping("/quizzes")
    public ResponseEntity<List<Quiz>> getAllQuizzes() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }
    
    @GetMapping("/quiz/{id}")
    public ResponseEntity<Quiz> getQuiz(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }
    
    @PostMapping("/quiz/{quizId}/submit")
    public ResponseEntity<?> submitQuiz(@PathVariable Long quizId, 
                                        @RequestParam Long studentId,
                                        @RequestBody SubmitQuizRequest request) {
        try {
            QuizAttempt attempt = quizAttemptService.submitQuizAttempt(quizId, studentId, request.getAnswers());
            return ResponseEntity.ok(attempt);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/attempts")
    public ResponseEntity<List<QuizAttempt>> getMyAttempts(@RequestParam Long studentId) {
        return ResponseEntity.ok(quizAttemptService.getStudentAttempts(studentId));
    }
    
    @GetMapping("/weak-topics")
    public ResponseEntity<Map<String, Object>> getWeakTopics(@RequestParam Long studentId) {
        return ResponseEntity.ok(weakTopicDetectorService.analyzeWeakTopics(studentId));
    }
    
    @GetMapping("/attempt/{attemptId}/analysis")
    public ResponseEntity<Map<String, Object>> getAttemptAnalysis(@PathVariable Long attemptId) {
        return ResponseEntity.ok(quizAnalysisService.analyzeAttempt(attemptId));
    }
    
    @GetMapping("/attempt/{attemptId}/feedback")
    public ResponseEntity<Map<String, Object>> getDetailedFeedback(@PathVariable Long attemptId) {
        return ResponseEntity.ok(quizAttemptService.getDetailedFeedback(attemptId));
    }
}
