package com.workit.domain.wallet.service;

import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.dto.response.WalletResponse;

public interface WalletService {

    void createWallet(Long userId);

    WalletResponse getMyWallet(Long userId);

    ChargeResponse charge(Long userId, ChargeRequest request);

    RefundResponse refund(Long userId, RefundRequest request);
}