package com.examplatform.dto;

import lombok.Data;

@Data
public class CreateQuizRequest {
    private String topic;
    private int numberOfQuestions;
}
