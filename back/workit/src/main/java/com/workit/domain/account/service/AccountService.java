package com.workit.domain.account.service;

import com.workit.domain.account.dto.response.AccountResponse;
import com.workit.domain.account.dto.response.AvailableAccountResponse;
import com.workit.domain.account.dto.response.DeleteAccountResponse;
import com.workit.domain.account.dto.response.PrimaryAccountResponse;

import java.util.List;

public interface AccountService {

    List<AvailableAccountResponse> getAvailableAccounts(Long userId);

    List<AccountResponse> getMyAccounts(Long userId);

    List<AccountResponse> linkAccounts(Long userId, List<Long> linkableAccountIds);

    PrimaryAccountResponse setPrimaryAccount(Long userId, Long accountId);

    DeleteAccountResponse deleteAccount(Long userId, Long accountId);
}