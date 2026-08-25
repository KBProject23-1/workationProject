package com.workit.domain.payment.service;

import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.ledger.service.LedgerService;
import com.workit.domain.ledger.vo.LedgerEntryVO;
import com.workit.domain.payment.TransactionStatus;
import com.workit.domain.payment.gateway.PgAuthResult;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.PaymentResponse;
import com.workit.domain.transaction.exception.TransactionErrorCode;
import com.workit.domain.transaction.mapper.TransactionMapper;
import com.workit.domain.transaction.vo.TransactionVO;
import com.workit.domain.wallet.mapper.WalletMapper;
import com.workit.domain.wallet.vo.WalletVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.workit.global.constant.PaymentPolicy.MIN_CHARGE_AMOUNT;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제의 DB 기입 구간만 짧게 트랜잭션으로 감싸는 협력자.
 *
 * 카드결제(PG 2단계)는 PG 호출(authorize/capture) 을 이 클래스 밖(PaymentServiceImpl.payWithCard)에서,
 * 트랜잭션 없이 수행한다. PG 호출을 @Transactional 안에 두면
 *   1) 커넥션을 PG 왕복 지연(~80ms+) 동안 점유해 처리량이 떨어지고,
 *   2) PG 성공 이후 DB 예외(데드락 등)로 트랜잭션이 롤백->재시도되면 이미 성공한 PG 호출이 중복 실행돼
 *      카드가 이중승인/이중매입될 수 있다(DeadlockRetrier 는 "롤백=재시도 안전"을 전제하는데,
 *      PG 호출처럼 트랜잭션 밖 시스템에 남는 부작용은 롤백으로 되돌릴 수 없어 이 전제가 깨진다).
 * 지갑결제(payWithWallet)는 외부 부작용이 없어 원래도 안전하지만, pay() 가 더 이상 메서드 전체를
 * @Transactional 로 감싸지 않으므로(카드 분기 때문에) 클래스 내부 self-invocation 으로는 프록시가 걸리지
 * 않는 스프링 @Transactional 특성상 이 협력자를 거쳐야 트랜잭션이 정상 적용된다.
 */
@Service
@RequiredArgsConstructor
public class PaymentTransactionRecorder {

    private final WalletMapper walletMapper;
    private final AccountMapper accountMapper;
    private final TransactionMapper transactionMapper;
    private final LedgerService ledgerService;

    private static final String DEFAULT_CATEGORY = "기타";

    // ===== 지갑결제 (pay() 의 WALLET 분기) — 원자적 단일 트랜잭션 =====
    @Transactional
    public PaymentResponse payWithWallet(Long userId, PaymentRequest request) {
        BigDecimal amount = request.getAmount();

        WalletVO wallet = walletMapper.findByUserId(userId);
        if (wallet == null) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_WALLET_NOT_FOUND);
        }

        // 1) REQUESTED 결제 거래 생성 — 멱등키 중복이면 자동충전/차감 전에 먼저 거부(409)
        TransactionVO paymentTx = TransactionVO.forWalletPayment(userId, wallet, request);
        assignMerchantCategory(paymentTx);
        try {
            transactionMapper.insertTransaction(paymentTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_DUPLICATE_REQUEST);
        }

        // 2) 잔액 부족 시 자동충전 (주계좌 -> 지갑). 내부 서브거래로 별도 기록 + 원장 기입
        BigDecimal shortage = amount.subtract(wallet.getBalance());
        boolean isAutoCharged = shortage.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal autoChargedAmount = null;

        if (isAutoCharged) {
            // 부족분이 최소 충전금액(1만원)보다 적으면 1만원으로 채움
            BigDecimal actualChargeAmount = shortage.compareTo(MIN_CHARGE_AMOUNT) < 0
                    ? MIN_CHARGE_AMOUNT
                    : shortage;

            BankAccountVO primaryAccount = accountMapper.findPrimaryAccount(userId);
            if (primaryAccount == null) {
                throw new BusinessException(TransactionErrorCode.TRANSACTION_PRIMARY_ACCOUNT_NOT_FOUND_FOR_AUTO_CHARGE);
            }

            int accountUpdatedRows = accountMapper.decreaseBalance(primaryAccount.getId(), userId, actualChargeAmount);
            if (accountUpdatedRows == 0) {
                throw new BusinessException(TransactionErrorCode.TRANSACTION_INSUFFICIENT_ACCOUNT_BALANCE);
            }

            walletMapper.increaseBalance(userId, actualChargeAmount);
            autoChargedAmount = actualChargeAmount;

            // 자동충전은 내부 원자 충전이라 PAID 로 즉시 생성(forAutoCharge). 단, 잔액이 움직였으므로 원장은 기입한다.
            TransactionVO depositTx = TransactionVO.forAutoCharge(userId, wallet, primaryAccount, actualChargeAmount);
            transactionMapper.insertTransaction(depositTx);
            BigDecimal accBalanceAfter = primaryAccount.getBalance().subtract(actualChargeAmount);
            BigDecimal walBalanceAfter = wallet.getBalance().add(actualChargeAmount);
            ledgerService.post(depositTx.getId(),
                    LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_BANK, primaryAccount.getId(), actualChargeAmount, accBalanceAfter),
                    LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), actualChargeAmount, walBalanceAfter));
        }

        // 3) 지갑 차감
        int walletUpdatedRows = walletMapper.decreaseBalance(userId, amount);
        if (walletUpdatedRows == 0) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_INSUFFICIENT_WALLET_BALANCE);
        }

        WalletVO updatedWallet = walletMapper.findByUserId(userId);

        // 4) 결제 원장: WALLET DEBIT(나감) / MERCHANT CREDIT(들어옴). 가맹점 잔액은 미보유 -> balance_after null
        ledgerService.post(paymentTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_WALLET, wallet.getId(), amount, updatedWallet.getBalance()),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_MERCHANT, paymentTx.getMerchantId(), amount, null));

        // 5) PAID 로 전이
        markPaid(paymentTx, userId);

        return PaymentResponse.ofWallet(paymentTx, updatedWallet.getBalance(), isAutoCharged, autoChargedAmount);
    }

    // ===== 카드결제 1단계: REQUESTED 생성 (짧은 트랜잭션, PG 호출 전 즉시 커밋) =====
    @Transactional
    public void createRequestedCardPayment(TransactionVO paymentTx) {
        try {
            transactionMapper.insertTransaction(paymentTx);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(TransactionErrorCode.TRANSACTION_DUPLICATE_REQUEST);
        }
    }

    // ===== 카드결제 2단계: PG 승인 결과 반영 (REQUESTED -> AUTHORIZED) =====
    @Transactional
    public void recordCardAuthorization(TransactionVO paymentTx, Long userId, PgAuthResult auth) {
        assertTransition(paymentTx.getStatus(), TransactionStatus.AUTHORIZED);
        transactionMapper.applyPgAuthorization(paymentTx.getId(), userId, auth.getPgTransactionId(), auth.getApprovalNumber());
        paymentTx.setPgTransactionId(auth.getPgTransactionId());
        paymentTx.setApprovedNumber(auth.getApprovalNumber());
        paymentTx.setStatus(TransactionStatus.AUTHORIZED.name());
    }

    // ===== 카드결제 3단계: 매입 완료 반영 (원장 기입 + AUTHORIZED -> PAID) =====
    @Transactional
    public void recordCardCaptureAndComplete(TransactionVO paymentTx, Long userId, BigDecimal amount) {
        // 원장: CARD DEBIT / MERCHANT CREDIT. 카드는 외부 발급사 자금이라 내부 잔액 이동 없음 -> balance_after null
        ledgerService.post(paymentTx.getId(),
                LedgerEntryVO.debit(LedgerEntryVO.ACCOUNT_CARD, paymentTx.getCardId(), amount, null),
                LedgerEntryVO.credit(LedgerEntryVO.ACCOUNT_MERCHANT, paymentTx.getMerchantId(), amount, null));

        markPaid(paymentTx, userId);
    }

    // ===== 카드결제 실패 처리 =====
    // REQUESTED insert 가 이미 별도 트랜잭션으로 커밋됐으므로, 과거처럼 전체 롤백으로 흔적을 지울 수 없다.
    // 대신 상태머신의 정식 종료 상태(FAILED)로 명시적으로 남겨 감사 추적을 보존한다.
    @Transactional
    public void markCardPaymentFailed(TransactionVO paymentTx, Long userId) {
        assertTransition(paymentTx.getStatus(), TransactionStatus.FAILED);
        transactionMapper.updateStatus(paymentTx.getId(), userId, TransactionStatus.FAILED.name(), null);
        paymentTx.setStatus(TransactionStatus.FAILED.name());
    }

    /**
     * 결제 시점의 가맹점 카테고리를 거래에 스냅샷으로 저장한다.
     * 나중에 가맹점 정보가 바뀌거나 삭제돼도 거래 당시 분류가 남도록 조회 시 조인이 아닌 저장 값을 쓴다.
     * INSERT 가 category_assigned 를 명시적으로 넣어 null 이면 컬럼 DEFAULT('기타')가 무시되므로,
     * merchantId 가 없거나 매칭 가맹점이 없으면 여기서 '기타'로 채워 의미 있는 기본값을 남긴다.
     */
    public void assignMerchantCategory(TransactionVO tx) {
        String category = tx.getMerchantId() != null
                ? transactionMapper.findMerchantCategoryById(tx.getMerchantId())
                : null;
        tx.setCategoryAssigned(category != null ? category : DEFAULT_CATEGORY);
    }

    /** REQUESTED/AUTHORIZED -> PAID 전이를 규칙 검증과 함께 수행. */
    private void markPaid(TransactionVO tx, Long userId) {
        assertTransition(tx.getStatus(), TransactionStatus.PAID);
        LocalDateTime approvedAt = LocalDateTime.now();
        transactionMapper.updateStatus(tx.getId(), userId, TransactionStatus.PAID.name(), approvedAt);
        tx.setStatus(TransactionStatus.PAID.name());
        tx.setApprovedAt(approvedAt);
    }

    /** 현재 상태에서 target 전이가 상태머신 규칙상 합법인지 검증. 위반 시 롤백(프로그래밍 오류). */
    private void assertTransition(String currentStatus, TransactionStatus target) {
        if (!TransactionStatus.from(currentStatus).canTransitionTo(target)) {
            throw new IllegalStateException("불법 상태 전이: " + currentStatus + " -> " + target);
        }
    }
}
