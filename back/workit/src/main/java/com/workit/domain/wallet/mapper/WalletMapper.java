package com.workit.domain.wallet.mapper;

import com.workit.domain.wallet.vo.WalletVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface WalletMapper {

    WalletVO findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(@Param("userId") Long userId);

    void insertWallet(@Param("userId") Long userId);

    void increaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    int decreaseBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}