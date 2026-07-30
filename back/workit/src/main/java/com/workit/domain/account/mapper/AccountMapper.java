package com.workit.domain.account.mapper;

import com.workit.domain.account.vo.BankAccountVO;
import com.workit.domain.account.vo.LinkableAccountVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AccountMapper {

    // 연동 가능한 계좌 후보 조회 (is_linked = false, banks 조인)
    List<LinkableAccountVO> findAvailableAccounts(@Param("userId") Long userId);

    LinkableAccountVO findLinkableAccountById(@Param("linkableAccountId") Long linkableAccountId,
                                              @Param("userId") Long userId);

    void markLinkableAccountAsLinked(@Param("linkableAccountId") Long linkableAccountId);

    void restoreLinkableAccount(@Param("userId") Long userId,
                                @Param("bankCode") String bankCode,
                                @Param("accountNumber") String accountNumber);

    // 연동된 내 계좌 목록/단건 조회
    List<BankAccountVO> findMyAccounts(@Param("userId") Long userId);

    BankAccountVO findAccountById(@Param("accountId") Long accountId, @Param("userId") Long userId);

    int countActiveAccounts(@Param("userId") Long userId);

    // 계좌 연동(등록)
    void insertAccount(BankAccountVO account);

    // 주 계좌 변경
    void clearPrimaryAccount(@Param("userId") Long userId);

    void setPrimaryAccount(@Param("accountId") Long accountId, @Param("userId") Long userId);

    // 계좌 삭제
    void deleteAccount(@Param("accountId") Long accountId, @Param("userId") Long userId);

    // 계좌 잔액 변경
    int decreaseBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    int  increaseBalance(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    BankAccountVO findPrimaryAccount(@Param("userId") Long userId);
}