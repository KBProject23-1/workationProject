package com.workit.domain.card.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NicknameUpdateResponse {
    private Long cardId;
    private String cardName;
    private LocalDateTime updatedAt;
}