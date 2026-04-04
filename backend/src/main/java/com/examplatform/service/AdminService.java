package com.examplatform.service;

import com.examplatform.model.Quiz;
import com.examplatform.model.User;
import com.examplatform.repository.QuizAttemptRepository;
import com.examplatform.repository.QuizRepository;
import com.examplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    
    public AdminService(UserRepository userRepository, QuizRepository quizRepository,
                        QuizAttemptRepository quizAttemptRepository) {
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() == User.Role.INSTRUCTOR) {
            List<Quiz> quizzes = quizRepository.findByInstructorId(userId);
            for (Quiz quiz : quizzes) {
                quizAttemptRepository.deleteAll(quizAttemptRepository.findByQuizId(quiz.getId()));
            }
            quizRepository.deleteAll(quizzes);
        } else if (user.getRole() == User.Role.STUDENT) {
            quizAttemptRepository.deleteAll(quizAttemptRepository.findByStudentId(userId));
        }
        
        userRepository.delete(user);
    }
    
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }
    
    @Transactional
    public Quiz approveQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
            .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quiz.setApproved(true);
        return quizRepository.save(quiz);
    }
}
