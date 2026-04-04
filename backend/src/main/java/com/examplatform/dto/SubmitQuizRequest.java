package com.examplatform.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SubmitQuizRequest {
    private Map<Long, String> answers;
}
