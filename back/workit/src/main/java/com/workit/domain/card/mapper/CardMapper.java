package com.workit.domain.card.mapper;

import com.workit.domain.card.vo.CardVO;
import com.workit.domain.card.vo.LinkableCardVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CardMapper {

    // 연동 가능한 카드 후보 조회 (아직 연동 안 했거나, 삭제해서 재연동 가능한 것)
    List<LinkableCardVO> findAvailableCards(@Param("userId") Long userId);

    LinkableCardVO findLinkableCardById(@Param("linkableCardId") Long linkableCardId,
                                        @Param("userId") Long userId);

    void markLinkableCardAsLinked(@Param("linkableCardId") Long linkableCardId);

    // 예전에 등록했다가 삭제한 카드가 있는지 확인 (같은 카드번호 기준)
    CardVO findDeletedCardByNumber(@Param("userId") Long userId, @Param("cardNumber") String cardNumber);

    // 삭제됐던 카드를 다시 살리기 (id 유지)
    void restoreCard(@Param("cardId") Long cardId,
                     @Param("userId") Long userId,
                     @Param("isPrimary") boolean isPrimary,
                     @Param("cardCompanyCode") String cardCompanyCode,
                     @Param("cardName") String cardName,
                     @Param("cardClassification") String cardClassification,
                     @Param("cardType") String cardType);

    // 연동된 내 카드 목록/단건 조회
    List<CardVO> findMyCards(@Param("userId") Long userId);

    CardVO findCardById(@Param("cardId") Long cardId, @Param("userId") Long userId);

    int countActiveCards(@Param("userId") Long userId);

    // 카드 신규 등록
    void insertCard(CardVO card);

    // 대표 카드 변경
    void clearPrimaryCard(@Param("userId") Long userId);

    void setPrimaryCard(@Param("cardId") Long cardId, @Param("userId") Long userId);

    // 별칭 수정
    void updateCardNickname(@Param("cardId") Long cardId, @Param("userId") Long userId,
                            @Param("cardNickname") String cardNickname);

    // 카드 삭제(소프트)
    void deleteCard(@Param("cardId") Long cardId, @Param("userId") Long userId);
}