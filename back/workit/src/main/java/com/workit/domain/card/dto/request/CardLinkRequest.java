package com.workit.domain.card.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CardLinkRequest {
    private List<Long> linkableCardIds;
}