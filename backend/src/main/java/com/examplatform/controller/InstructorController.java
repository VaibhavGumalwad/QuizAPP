package com.examplatform.controller;

import com.examplatform.dto.CreateQuizRequest;
import com.examplatform.model.Quiz;
import com.examplatform.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instructor")
public class InstructorController {
    private final QuizService quizService;
    
    public InstructorController(QuizService quizService) {
        this.quizService = quizService;
    }
    
    @PostMapping("/quiz")
    public ResponseEntity<?> createQuiz(@RequestBody CreateQuizRequest request, 
                                        @RequestParam Long instructorId) {
        try {
            System.out.println("Creating quiz - Topic: " + request.getTopic() + ", Questions: " + request.getNumberOfQuestions());
            Quiz quiz = quizService.createQuiz(request.getTopic(), request.getNumberOfQuestions(), instructorId);
            System.out.println("Quiz created successfully with ID: " + quiz.getId());
            return ResponseEntity.ok(quiz);
        } catch (Exception e) {
            System.err.println("Error creating quiz: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/quizzes")
    public ResponseEntity<List<Quiz>> getMyQuizzes(@RequestParam Long instructorId) {
        return ResponseEntity.ok(quizService.getQuizzesByInstructor(instructorId));
    }
    
    @DeleteMapping("/quiz/{quizId}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId, @RequestParam Long instructorId) {
        try {
            quizService.deleteQuiz(quizId, instructorId);
            return ResponseEntity.ok(Map.of("message", "Quiz deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/quiz/{quizId}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable Long quizId) {
        try {
            return ResponseEntity.ok(quizService.getQuizResults(quizId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
