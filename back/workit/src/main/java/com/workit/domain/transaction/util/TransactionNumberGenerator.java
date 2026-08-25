package com.workit.domain.transaction.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class TransactionNumberGenerator {

    private TransactionNumberGenerator() {
    }

    //우리 서비스 거래번호 생성: TXN-{yyyyMMdd}-{4자리 랜덤}
    public static String generateTransactionNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(0, 10000);
        return String.format("TXN-%s-%04d", datePart, randomPart);
    }

    // 카드 승인번호 생성: 10자리 순수 숫자 (Mock)

    public static String generateApprovalNumber() {
        long number = ThreadLocalRandom.current().nextLong(0, 10_000_000_000L);
        return String.format("%010d", number);
    }
}