package com.workit.domain.account.service;

import com.workit.domain.account.dto.response.AccountResponse;
import com.workit.domain.account.dto.response.AvailableAccountResponse;
import com.workit.domain.account.dto.response.DeleteAccountResponse;
import com.workit.domain.account.dto.response.PrimaryAccountResponse;
import com.workit.domain.account.exception.AccountErrorCode;
import com.workit.domain.account.mapper.AccountMapper;
import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.account.vo.LinkableAccountVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;

    @Override
    public List<AvailableAccountResponse> getAvailableAccounts(Long userId) {
        List<LinkableAccountVO> linkableAccounts = accountMapper.findAvailableAccounts(userId);
        return linkableAccounts.stream()
                .map(AvailableAccountResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<AccountResponse> getMyAccounts(Long userId) {
        List<BankAccountVO> accounts = accountMapper.findMyAccounts(userId);
        return accounts.stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AccountResponse> linkAccounts(Long userId, List<Long> linkableAccountIds) {
        boolean hasExistingAccount = accountMapper.countActiveAccounts(userId) > 0;

        List<AccountResponse> results = new ArrayList<>();

        for (int i = 0; i < linkableAccountIds.size(); i++) {
            Long linkableId = linkableAccountIds.get(i);

            LinkableAccountVO linkable = accountMapper.findLinkableAccountById(linkableId, userId);
            if (linkable == null) {
                throw new BusinessException(AccountErrorCode.ACCOUNT_LINKABLE_NOT_FOUND);
            }

            boolean isPrimary = !hasExistingAccount && i == 0;

            BankAccountVO newAccount = new BankAccountVO();
            newAccount.setUserId(userId);
            newAccount.setBankCode(linkable.getBankCode());
            newAccount.setAccountNumber(linkable.getAccountNumber());
            newAccount.setProductName(linkable.getProductName());
            newAccount.setBalance(generateMockBalance());
            newAccount.setIsPrimary(isPrimary);

            accountMapper.insertAccount(newAccount);
            accountMapper.markLinkableAccountAsLinked(linkableId);

            BankAccountVO saved = accountMapper.findAccountById(newAccount.getId(), userId);
            results.add(AccountResponse.from(saved));
        }

        return results;
    }

    @Override
    @Transactional
    public PrimaryAccountResponse setPrimaryAccount(Long userId, Long accountId) {
        BankAccountVO account = accountMapper.findAccountById(accountId, userId);
        if (account == null) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }

        accountMapper.clearPrimaryAccount(userId);
        accountMapper.setPrimaryAccount(accountId, userId);

        PrimaryAccountResponse response = new PrimaryAccountResponse();
        response.setAccountId(accountId);
        response.setIsPrimary(true);
        return response;
    }

    @Override
    @Transactional
    public DeleteAccountResponse deleteAccount(Long userId, Long accountId) {
        BankAccountVO account = accountMapper.findAccountById(accountId, userId);
        if (account == null) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(account.getIsPrimary())) {
            throw new BusinessException(AccountErrorCode.ACCOUNT_PRIMARY_DELETE_NOT_ALLOWED);
        }

        accountMapper.deleteAccount(accountId, userId);
        accountMapper.restoreLinkableAccount(userId, account.getBankCode(), account.getAccountNumber());

        DeleteAccountResponse response = new DeleteAccountResponse();
        response.setAccountId(accountId);
        response.setIsDeleted(true);
        return response;
    }

    private BigDecimal generateMockBalance() {
        int amount = ThreadLocalRandom.current().nextInt(100_000, 5_000_001);
        return BigDecimal.valueOf(amount);
    }
}