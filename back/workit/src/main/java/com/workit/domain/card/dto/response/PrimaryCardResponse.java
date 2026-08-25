package com.workit.domain.card.dto.response;

import lombok.Data;

@Data
public class PrimaryCardResponse {
    private Long cardId;
    private Boolean isPrimary;
}