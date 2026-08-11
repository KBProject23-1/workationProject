package com.workit.domain.wallet.service;

import com.workit.domain.wallet.dto.response.WalletResponse;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 지갑 라이프사이클(생성/조회) 담당.
 * 충전/환불 등 결제 오케스트레이션은 payment 도메인(PaymentService)으로 이관됨.
 */
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    @Override
    public void createWallet(Long userId) {
        if (walletMapper.existsByUserId(userId)) {
            return;
        }
        walletMapper.insertWallet(userId);
    }

    @Override
    public WalletResponse getMyWallet(Long userId) {
        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            walletMapper.insertWallet(userId);
            wallet = walletMapper.findByUserId(userId);
        }
        return WalletResponse.from(wallet);
    }
}
