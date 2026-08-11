package com.workit.domain.payment.service;

import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;

/**
 * 결제 오케스트레이션 진입점.
 * 충전(charge)·환불(refund)·결제(pay)의 공통 흐름을 payment 도메인이 소유한다.
 */
public interface PaymentService {

    ChargeResponse charge(Long userId, ChargeRequest request);

    RefundResponse refund(Long userId, RefundRequest request);

    PaymentResponse pay(Long userId, PaymentRequest request);
}
