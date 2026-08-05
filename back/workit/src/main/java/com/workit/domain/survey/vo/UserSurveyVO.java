package com.workit.domain.survey.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserSurveyVO {

    private Long surveyId;
    private Long userId;
    private Long workationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

