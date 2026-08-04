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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
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
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(cardService.getAvailableCards(userId));
    }

    /** 연동된 내 카드 목록 조회 */
    @GetMapping
    public ResponseEntity<CommonResponse<List<CardResponse>>> getMyCards(
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(cardService.getMyCards(userId));
    }

    /** 카드 등록(연동) */
    @PostMapping
    public ResponseEntity<CommonResponse<List<CardResponse>>> linkCards(
            @RequestBody CardLinkRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.created(cardService.linkCards(userId, requestBody.getLinkableCardIds()));
    }

    /** 대표 카드 설정 */
    @PatchMapping("/{cardsId}/primary")
    public ResponseEntity<CommonResponse<PrimaryCardResponse>> setPrimaryCard(
            @PathVariable Long cardsId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(cardService.setPrimaryCard(userId, cardsId));
    }

    /** 카드 별칭 수정 */
    @PatchMapping("/{cardsId}/nickname")
    public ResponseEntity<CommonResponse<NicknameUpdateResponse>> updateNickname(
            @PathVariable Long cardsId,
            @RequestBody CardNicknameRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(cardService.updateNickname(userId, cardsId, requestBody.getCardNickname()));
    }

    /** 카드 삭제(소프트) */
    @DeleteMapping("/{cardsId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long cardsId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        cardService.deleteCard(userId, cardsId);
        return GlobalResponseFactory.noContent();
    }
}