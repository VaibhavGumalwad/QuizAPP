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
import java.util.stream.Collectors;

@Service
public class QuizService {
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final AIQuizGeneratorService aiQuizGeneratorService;
    
    public QuizService(QuizRepository quizRepository, UserRepository userRepository,
                       QuizAttemptRepository quizAttemptRepository,
                       AIQuizGeneratorService aiQuizGeneratorService) {
        this.quizRepository = quizRepository;
        this.userRepository = userRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.aiQuizGeneratorService = aiQuizGeneratorService;
    }
    
    @Transactional
    public Quiz createQuiz(String topic, int numberOfQuestions, Long instructorId) {
        User instructor = userRepository.findById(instructorId)
            .orElseThrow(() -> new RuntimeException("Instructor not found"));
        
        List<Map<String, Object>> generatedQuestions = aiQuizGeneratorService.generateQuiz(topic, numberOfQuestions);
        
        Quiz quiz = new Quiz();
        quiz.setTitle("Quiz on " + topic);
        quiz.setTopic(topic);
        quiz.setInstructor(instructor);
        
        for (Map<String, Object> qData : generatedQuestions) {
            Question question = new Question();
            question.setQuiz(quiz);
            question.setQuestionText((String) qData.get("question"));
            question.setOptions((List<String>) qData.get("options"));
            question.setCorrectAnswer((String) qData.get("correctAnswer"));
            quiz.getQuestions().add(question);
        }
        
        return quizRepository.save(quiz);
    }
    
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findByApprovedTrue();
    }
    
    public List<Quiz> getQuizzesByInstructor(Long instructorId) {
        return quizRepository.findByInstructorId(instructorId);
    }
    
    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
    }
    
    @Transactional
    public void deleteQuiz(Long quizId, Long instructorId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        // Verify the quiz belongs to this instructor
        if (!quiz.getInstructor().getId().equals(instructorId)) {
            throw new RuntimeException("You are not authorized to delete this quiz");
        }
        
        // Delete all attempts for this quiz first
        quizAttemptRepository.deleteAll(quizAttemptRepository.findByQuizId(quizId));
        
        // Now delete the quiz (cascade will delete questions)
        quizRepository.delete(quiz);
        System.out.println("Quiz deleted: " + quizId + " by instructor: " + instructorId);
    }
    
    public Map<String, Object> getQuizResults(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        
        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizId(quizId);
        
        // Fix old attempts that don't have totalQuestions set
        for (QuizAttempt attempt : attempts) {
            if (attempt.getTotalQuestions() == null || attempt.getTotalQuestions() == 0) {
                attempt.setTotalQuestions(quiz.getQuestions().size());
                quizAttemptRepository.save(attempt);
            }
        }
        
        List<Map<String, Object>> attemptData = attempts.stream().map(attempt -> {
            Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", attempt.getId());
            data.put("studentName", attempt.getStudent().getUsername());
            data.put("score", attempt.getScore());
            data.put("totalQuestions", attempt.getTotalQuestions());
            data.put("attemptedAt", attempt.getAttemptedAt());
            return data;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("quizTitle", quiz.getTitle());
        result.put("attempts", attemptData);
        return result;
    }
}
