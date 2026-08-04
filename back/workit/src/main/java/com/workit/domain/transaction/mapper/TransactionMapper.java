package com.workit.domain.transaction.mapper;

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

    // 단건 상세 조회 (좌표 포함, merchants 조인)
    TransactionVO findTransactionDetailById(@Param("transactionId") Long transactionId,
                                            @Param("userId") Long userId);

    // 매출전표용 조회 (merchants + cards 조인)
    TransactionVO findTransactionForReceipt(@Param("transactionId") Long transactionId,
                                            @Param("userId") Long userId);

    // 거래 생성 (충전/환불/카드결제 공용)
    void insertTransaction(TransactionVO transaction);

    // 취소 대상 조회
    TransactionVO findTransactionForCancel(@Param("transactionId") Long transactionId,
                                           @Param("userId") Long userId);

    // 거래 취소 처리
    void cancelTransaction(@Param("transactionId") Long transactionId);
}