package com.examplatform.service;

import com.examplatform.model.Question;
import com.examplatform.model.Quiz;
import com.examplatform.model.QuizAttempt;
import com.examplatform.model.User;
import com.examplatform.repository.QuizAttemptRepository;
import com.examplatform.repository.QuizRepository;
import com.examplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
public class QuizAttemptService {
    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final AIFeedbackService aiFeedbackService;
    
    public QuizAttemptService(QuizAttemptRepository quizAttemptRepository, 
                              QuizRepository quizRepository, UserRepository userRepository,
                              AIFeedbackService aiFeedbackService) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.aiFeedbackService = aiFeedbackService;
    }
    
    @Transactional
    public QuizAttempt submitQuizAttempt(Long quizId, Long studentId, Map<Long, String> answers) {
        if (quizAttemptRepository.existsByStudentIdAndQuizId(studentId, quizId)) {
            throw new RuntimeException("Quiz already attempted");
        }
        
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found"));
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        int score = calculateScore(quiz, answers);
        
        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setAnswers(answers);
        attempt.setScore(score);
        attempt.setTotalQuestions(quiz.getQuestions().size());
        
        return quizAttemptRepository.save(attempt);
    }
    
    private int calculateScore(Quiz quiz, Map<Long, String> answers) {
        int correct = 0;
        System.out.println("\n=== Calculating Score ===");
        System.out.println("Total questions: " + quiz.getQuestions().size());
        System.out.println("Student answers: " + answers.size());
        
        for (Question question : quiz.getQuestions()) {
            String studentAnswer = answers.get(question.getId());
            String correctAnswer = question.getCorrectAnswer();
            
            System.out.println("\nQuestion ID: " + question.getId());
            System.out.println("Question: " + question.getQuestionText());
            System.out.println("Student Answer: " + studentAnswer);
            System.out.println("Correct Answer: " + correctAnswer);
            
            if (studentAnswer != null && studentAnswer.equals(correctAnswer)) {
                correct++;
                System.out.println("Result: CORRECT ✓");
            } else {
                System.out.println("Result: WRONG ✗");
            }
        }
        
        System.out.println("\nFinal Score: " + correct + "/" + quiz.getQuestions().size());
        System.out.println("======================\n");
        return correct;
    }
    
    public List<QuizAttempt> getStudentAttempts(Long studentId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentId(studentId);
        // Fix old attempts that don't have totalQuestions set
        for (QuizAttempt attempt : attempts) {
            if (attempt.getTotalQuestions() == null || attempt.getTotalQuestions() == 0) {
                Quiz quiz = quizRepository.findById(attempt.getQuiz().getId()).orElse(null);
                if (quiz != null && quiz.getQuestions() != null) {
                    attempt.setTotalQuestions(quiz.getQuestions().size());
                    quizAttemptRepository.save(attempt);
                }
            }
        }
        return attempts;
    }
    
    public Map<String, Object> getDetailedFeedback(Long attemptId) {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Quiz attempt not found"));
        
        return aiFeedbackService.generateDetailedFeedback(attempt);
    }
}
