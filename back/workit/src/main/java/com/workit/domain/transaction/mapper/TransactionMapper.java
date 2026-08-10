package com.workit.domain.transaction.mapper;

import com.workit.domain.transaction.vo.TransactionSummaryVO;
import com.workit.domain.transaction.vo.TransactionVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionMapper {

    // 목록 조회 (필터링, 최신순, 페이징)
    List<TransactionVO> findTransactions(@Param("userId") Long userId,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate,
                                         @Param("paymentSourceType") String paymentSourceType,
                                         @Param("transactionType") String transactionType,
                                         @Param("cardId") Long cardId,
                                         @Param("offset") int offset,
                                         @Param("size") int size);

    // 목록 페이징용 전체 건수. 필터 조건이 목록과 같아야 한다
    long countTransactions(@Param("userId") Long userId,
                           @Param("startDate") String startDate,
                           @Param("endDate") String endDate,
                           @Param("paymentSourceType") String paymentSourceType,
                           @Param("transactionType") String transactionType,
                           @Param("cardId") Long cardId);

    // 조회 기간 전체 집계 (결제 합계 / 충전 합계). 페이징과 무관하게 필터 조건 전체를 기준으로 계산한다
    TransactionSummaryVO findTransactionSummary(@Param("userId") Long userId,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate,
                                                @Param("paymentSourceType") String paymentSourceType,
                                                @Param("cardId") Long cardId);

    // 단건 상세 조회 (좌표 포함, merchants 조인)
    TransactionVO findTransactionDetailById(@Param("transactionId") Long transactionId,
                                            @Param("userId") Long userId);

    // 매출전표용 조회 (merchants + cards 조인)
    TransactionVO findTransactionForReceipt(@Param("transactionId") Long transactionId,
                                            @Param("userId") Long userId);

    // 거래 생성 (충전/환불/카드결제 공용)
    void insertTransaction(TransactionVO transaction);

    // 상태 전이 (REQUESTED -> AUTHORIZED/PAID 등). approvedAt 은 승인 성공 시점에만 채운다(그 외 null 전달).
    int updateStatus(@Param("transactionId") Long transactionId,
                     @Param("status") String status,
                     @Param("approvedAt") java.time.LocalDateTime approvedAt);

    // 취소 대상 조회
    TransactionVO findTransactionForCancel(@Param("transactionId") Long transactionId,
                                           @Param("userId") Long userId);

    // 거래 취소 처리
    void cancelTransaction(@Param("transactionId") Long transactionId);
}