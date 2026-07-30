package com.workit.domain.wallet.dto.response;

import com.workit.domain.wallet.vo.WalletVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletResponse {
    private Long walletId;
    private Long userId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;

    public static WalletResponse from(WalletVO vo) {
        WalletResponse response = new WalletResponse();
        response.setWalletId(vo.getId());
        response.setUserId(vo.getUserId());
        response.setBalance(vo.getBalance());
        response.setUpdatedAt(vo.getUpdatedAt());
        return response;
    }
}