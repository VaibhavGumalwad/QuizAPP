package com.examplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String topic;
    
    @ManyToOne
    @JoinColumn(name = "instructor_id", nullable = false)
    @JsonIgnoreProperties({"password", "email", "createdAt"})
    private User instructor;
    
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("quiz")
    private List<Question> questions = new ArrayList<>();
    
    @Column(nullable = false)
    private Boolean approved = false;
    
    private LocalDateTime createdAt = LocalDateTime.now();
}
