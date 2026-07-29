package com.workit.domain.card.service;

import com.workit.domain.card.dto.response.AvailableCardResponse;
import com.workit.domain.card.dto.response.CardResponse;
import com.workit.domain.card.dto.response.NicknameUpdateResponse;
import com.workit.domain.card.dto.response.PrimaryCardResponse;

import java.util.List;

public interface CardService {

    List<AvailableCardResponse> getAvailableCards(Long userId);

    List<CardResponse> getMyCards(Long userId);

    List<CardResponse> linkCards(Long userId, List<Long> linkableCardIds);

    PrimaryCardResponse setPrimaryCard(Long userId, Long cardId);

    NicknameUpdateResponse updateNickname(Long userId, Long cardId, String cardNickname);

    void deleteCard(Long userId, Long cardId);
}