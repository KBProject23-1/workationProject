package com.workit.domain.wallet.service;

import com.workit.domain.wallet.dto.response.WalletResponse;

import java.math.BigDecimal;

public interface WalletService {

    void createWallet(Long userId);

    WalletResponse getMyWallet(Long userId);

    /**
     * 전자지갑 잔액 순수 조회 (읽기 전용 — 생성/변경 부작용 없음)
     * - 회원 탈퇴 등 다른 도메인에서 잔액을 확인할 때 getMyWallet() 대신 사용한다
     *   (getMyWallet() 은 지갑 미존재 시 insert 하는 부작용이 있어 탈퇴 잔액 검증에는 부적합)
     * - 지갑이 없거나 잔액이 NULL 이면 null 반환 (호출 측에서 0 으로 간주)
     *
     * @return 현재 잔액 (BigDecimal), 지갑 미존재 시 null
     */
    BigDecimal getBalance(Long userId);
}
