package com.workit.domain.account.controller;

import com.workit.domain.account.dto.request.AccountLinkRequest;
import com.workit.domain.account.dto.response.AccountResponse;
import com.workit.domain.account.dto.response.AvailableAccountResponse;
import com.workit.domain.account.dto.response.DeleteAccountResponse;
import com.workit.domain.account.dto.response.PrimaryAccountResponse;
import com.workit.domain.account.service.AccountService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;

    /** 연동 가능한 계좌 후보 조회 */
    @GetMapping("/available")
    public ResponseEntity<CommonResponse<List<AvailableAccountResponse>>> getAvailableAccounts(
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(accountService.getAvailableAccounts(userId));
    }

    /** 연동된 내 계좌 목록 조회 */
    @GetMapping("/me")
    public ResponseEntity<CommonResponse<List<AccountResponse>>> getMyAccounts(
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(accountService.getMyAccounts(userId));
    }

    /** 계좌 등록(연동) */
    @PostMapping
    public ResponseEntity<CommonResponse<List<AccountResponse>>> linkAccounts(
            @RequestBody AccountLinkRequest requestBody,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.success(accountService.linkAccounts(userId, requestBody.getLinkableAccountIds())));
    }

    /** 주 계좌 변경 */
    @PatchMapping("/{accountId}/primary")
    public ResponseEntity<CommonResponse<PrimaryAccountResponse>> setPrimaryAccount(
            @PathVariable Long accountId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(accountService.setPrimaryAccount(userId, accountId));
    }

    /** 계좌 삭제(소프트) */
    @DeleteMapping("/{accountId}")
    public ResponseEntity<CommonResponse<DeleteAccountResponse>> deleteAccount(
            @PathVariable Long accountId,
            HttpServletRequest request
    ) {
        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;
        return GlobalResponseFactory.success(accountService.deleteAccount(userId, accountId));
    }
}