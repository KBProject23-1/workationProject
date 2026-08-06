package com.workit.domain.card.controller;

import com.workit.domain.card.dto.request.CardLinkRequest;
import com.workit.domain.card.dto.request.CardNicknameRequest;
import com.workit.domain.card.dto.response.AvailableCardResponse;
import com.workit.domain.card.dto.response.CardResponse;
import com.workit.domain.card.dto.response.NicknameUpdateResponse;
import com.workit.domain.card.dto.response.PrimaryCardResponse;
import com.workit.domain.card.service.CardService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Slf4j
public class CardController {

    private final CardService cardService;

    /** 연동 가능한 카드 후보 조회 */
    @GetMapping("/available")
    public ResponseEntity<CommonResponse<List<AvailableCardResponse>>> getAvailableCards(
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(cardService.getAvailableCards(userId));
    }

    /** 연동된 내 카드 목록 조회 */
    @GetMapping
    public ResponseEntity<CommonResponse<List<CardResponse>>> getMyCards(
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(cardService.getMyCards(userId));
    }

    /** 거래 내역 카드별 필터용 전체 카드 목록 (삭제된 카드 포함) */
    @GetMapping("/all")
    public ResponseEntity<CommonResponse<List<CardResponse>>> getAllCardsForFilter(
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(cardService.getAllCardsForFilter(userId));
    }

    /** 카드 등록(연동) */
    @PostMapping
    public ResponseEntity<CommonResponse<List<CardResponse>>> linkCards(
            @RequestBody CardLinkRequest requestBody,
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.created(cardService.linkCards(userId, requestBody.getLinkableCardIds()));
    }

    /** 대표 카드 설정 */
    @PatchMapping("/{cardsId}/primary")
    public ResponseEntity<CommonResponse<PrimaryCardResponse>> setPrimaryCard(
            @PathVariable Long cardsId,
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(cardService.setPrimaryCard(userId, cardsId));
    }

    /** 카드 별칭 수정 */
    @PatchMapping("/{cardsId}/nickname")
    public ResponseEntity<CommonResponse<NicknameUpdateResponse>> updateNickname(
            @PathVariable Long cardsId,
            @RequestBody CardNicknameRequest requestBody,
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(cardService.updateNickname(userId, cardsId, requestBody.getCardNickname()));
    }

    /** 카드 삭제(소프트) */
    @DeleteMapping("/{cardsId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long cardsId,
            @CurrentUser Long userId
    ) {
        cardService.deleteCard(userId, cardsId);
        return GlobalResponseFactory.noContent();
    }
}