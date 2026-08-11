package com.workit.domain.wallet.service;

import com.workit.domain.wallet.dto.response.WalletResponse;

public interface WalletService {

    void createWallet(Long userId);

    WalletResponse getMyWallet(Long userId);
}
