package com.workit.domain.card.service;

import com.workit.domain.card.dto.response.AvailableCardResponse;
import com.workit.domain.card.dto.response.CardResponse;
import com.workit.domain.card.dto.response.NicknameUpdateResponse;
import com.workit.domain.card.dto.response.PrimaryCardResponse;
import com.workit.domain.card.exception.CardErrorCode;
import com.workit.domain.card.mapper.CardMapper;
import com.workit.domain.card.vo.CardVO;
import com.workit.domain.card.vo.LinkableCardVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardMapper cardMapper;

    @Override
    public List<AvailableCardResponse> getAvailableCards(Long userId) {
        List<LinkableCardVO> linkableCards = cardMapper.findAvailableCards(userId);
        return linkableCards.stream()
                .map(AvailableCardResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> getMyCards(Long userId) {
        List<CardVO> cards = cardMapper.findMyCards(userId);
        return cards.stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CardResponse> getAllCardsForFilter(Long userId) {
        List<CardVO> cards = cardMapper.findAllCardsForFilter(userId);
        return cards.stream()
                .map(CardResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<CardResponse> linkCards(Long userId, List<Long> linkableCardIds) {
        boolean hasExistingCard = cardMapper.countActiveCards(userId) > 0;

        List<CardResponse> results = new ArrayList<>();

        for (int i = 0; i < linkableCardIds.size(); i++) {
            Long linkableId = linkableCardIds.get(i);

            LinkableCardVO linkable = cardMapper.findLinkableCardById(linkableId, userId);
            if (linkable == null) {
                throw new BusinessException(CardErrorCode.LINKABLE_CARD_NOT_FOUND);
            }

            boolean isPrimary = !hasExistingCard && i == 0;

            CardVO deletedCard = cardMapper.findDeletedCardByNumber(userId, linkable.getCardNumber());

            CardVO saved;
            if (deletedCard != null) {
                cardMapper.restoreCard(
                        deletedCard.getId(),
                        userId,
                        isPrimary,
                        linkable.getCardCompanyCode(),
                        linkable.getCardName(),
                        linkable.getCardClassification(),
                        linkable.getCardType()
                );
                saved = cardMapper.findCardById(deletedCard.getId(), userId);
            } else {
                CardVO newCard = new CardVO();
                newCard.setUserId(userId);
                newCard.setCardCompanyCode(linkable.getCardCompanyCode());
                newCard.setCardName(linkable.getCardName());
                newCard.setCardNumber(linkable.getCardNumber());
                newCard.setCardClassification(linkable.getCardClassification());
                newCard.setCardType(linkable.getCardType());
                newCard.setIsPrimary(isPrimary);

                try {
                    cardMapper.insertCard(newCard);
                } catch (DuplicateKeyException e) {
                    // UQ_cards_user_card_number: 이미 연동된 카드를 재연동 시도(더블클릭/재시도) -> 깔끔한 비즈니스 에러로 변환
                    throw new BusinessException(CardErrorCode.CARD_ALREADY_LINKED);
                }
                saved = cardMapper.findCardById(newCard.getId(), userId);
            }

            cardMapper.markLinkableCardAsLinked(linkableId);
            results.add(CardResponse.from(saved));
        }

        return results;
    }

    @Override
    @Transactional
    public PrimaryCardResponse setPrimaryCard(Long userId, Long cardId) {
        CardVO card = cardMapper.findCardById(cardId, userId);
        if (card == null) {
            throw new BusinessException(CardErrorCode.CARD_NOT_FOUND);
        }

        cardMapper.clearPrimaryCard(userId);
        cardMapper.setPrimaryCard(cardId, userId);

        PrimaryCardResponse response = new PrimaryCardResponse();
        response.setCardId(cardId);
        response.setIsPrimary(true);
        return response;
    }

    @Override
    @Transactional
    public NicknameUpdateResponse updateNickname(Long userId, Long cardId, String cardNickname) {
        if (cardNickname == null || cardNickname.trim().isEmpty()) {
            throw new BusinessException(CardErrorCode.CARD_NICKNAME_REQUIRED);
        }
        if (cardNickname.length() > 100) {
            throw new BusinessException(CardErrorCode.CARD_NICKNAME_TOO_LONG);
        }

        CardVO card = cardMapper.findCardById(cardId, userId);
        if (card == null) {
            throw new BusinessException(CardErrorCode.CARD_NOT_FOUND);
        }

        cardMapper.updateCardNickname(cardId, userId, cardNickname);
        CardVO updated = cardMapper.findCardById(cardId, userId);

        NicknameUpdateResponse response = new NicknameUpdateResponse();
        response.setCardId(cardId);
        response.setCardName(updated.getCardName());
        response.setUpdatedAt(updated.getUpdatedAt());
        return response;
    }

    @Override
    @Transactional
    public void deleteCard(Long userId, Long cardId) {
        CardVO card = cardMapper.findCardById(cardId, userId);
        if (card == null) {
            throw new BusinessException(CardErrorCode.CARD_NOT_FOUND);
        }

        boolean wasPrimary = Boolean.TRUE.equals(card.getIsPrimary());

        cardMapper.deleteCard(cardId, userId);

        if (wasPrimary) {
            List<CardVO> remainingCards = cardMapper.findMyCards(userId);
            if (!remainingCards.isEmpty()) {
                Long nextPrimaryId = remainingCards.get(0).getId();
                cardMapper.setPrimaryCard(nextPrimaryId, userId);
            }
        }
    }
}