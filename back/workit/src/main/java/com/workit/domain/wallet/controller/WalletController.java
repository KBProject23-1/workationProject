package com.workit.domain.wallet.controller;

import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.dto.response.WalletResponse;
import com.workit.domain.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    /** 지갑 조회 */
    @GetMapping("/me")
    public ResponseEntity<WalletResponse> getMyWallet(
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return ResponseEntity.ok(walletService.getMyWallet(userId));
    }

    /** 지갑 충전 */
    @PostMapping("/charge")
    public ResponseEntity<ChargeResponse> charge(
            @RequestBody ChargeRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return ResponseEntity.ok(walletService.charge(userId, requestBody));
    }

    /** 지갑 환불 */
    @PostMapping("/refund")
    public ResponseEntity<RefundResponse> refund(
            @RequestBody RefundRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return ResponseEntity.ok(walletService.refund(userId, requestBody));
    }
}