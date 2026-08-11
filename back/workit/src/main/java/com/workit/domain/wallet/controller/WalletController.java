package com.workit.domain.wallet.controller;

import com.workit.domain.wallet.dto.request.ChargeRequest;
import com.workit.domain.wallet.dto.request.RefundRequest;
import com.workit.domain.wallet.dto.response.ChargeResponse;
import com.workit.domain.wallet.dto.response.RefundResponse;
import com.workit.domain.wallet.dto.response.WalletResponse;
import com.workit.domain.payment.service.PaymentService;
import com.workit.domain.support.DeadlockRetrier;
import com.workit.domain.wallet.service.WalletService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;
    private final DeadlockRetrier deadlockRetrier;

    /** 지갑 조회 */
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<WalletResponse>> getMyWallet(
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(walletService.getMyWallet(userId));
    }

    /** 지갑 충전 */
    @PostMapping("/charge")
    public ResponseEntity<CommonResponse<ChargeResponse>> charge(
            @RequestBody ChargeRequest requestBody,
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(deadlockRetrier.execute(() -> paymentService.charge(userId, requestBody)));
    }

    /** 지갑 환불 */
    @PostMapping("/refund")
    public ResponseEntity<CommonResponse<RefundResponse>> refund(
            @RequestBody RefundRequest requestBody,
            @CurrentUser Long userId
    ) {
        return GlobalResponseFactory.success(deadlockRetrier.execute(() -> paymentService.refund(userId, requestBody)));
    }
}