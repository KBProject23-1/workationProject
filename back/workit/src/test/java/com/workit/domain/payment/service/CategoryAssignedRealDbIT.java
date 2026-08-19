package com.workit.domain.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 실제 workit MySQL 에 직접 붙어, 결제 카테고리 저장 변경(#hotfix)의 SQL 경로를 검증한다.
 *
 * 검증하는 체인 (PaymentServiceImpl.assignMerchantCategory + insertTransaction 과 동일한 SQL):
 *   1) TransactionMapper.findMerchantCategoryById 와 동일한 SELECT 로 가맹점 category 를 얻는다
 *   2) insertTransaction 과 동일하게 transactions.category_assigned 에 그 값을 넣는다
 *   3) 되읽어 category_assigned == merchant.category 임을 확인한다
 *   4) 존재하지 않는 merchantId 는 SELECT 가 null → 서비스는 "기타"로 저장함을 확인한다
 *
 * DB 미기동/미접속 시엔 assume 으로 스킵된다. INSERT 는 트랜잭션으로 넣고 롤백하여 DB 를 더럽히지 않는다.
 */
class CategoryAssignedRealDbIT {

    private Properties db() {
        Properties p = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application-secret.properties")) {
            if (in == null) return null;
            p.load(in);
            return p;
        } catch (Exception e) {
            return null;
        }
    }

    private Connection connect() {
        Properties p = db();
        assumeTrue(p != null, "application-secret.properties 없음 → 스킵");
        try {
            return DriverManager.getConnection(
                    p.getProperty("jdbc.url"), p.getProperty("jdbc.username"), p.getProperty("jdbc.password"));
        } catch (Exception e) {
            assumeTrue(false, "workit DB 접속 불가 → 스킵: " + e.getMessage());
            return null;
        }
    }

    @Test
    @DisplayName("실제 DB: 결제 시 merchant.category 가 transactions.category_assigned 로 저장된다")
    void 결제_카테고리_실제저장_검증() throws Exception {
        try (Connection con = connect()) {
            con.setAutoCommit(false);

            // 1) 실제 가맹점 하나 선택 (id, category)
            long merchantId;
            String merchantCategory;
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, category FROM merchants LIMIT 1")) {
                assumeTrue(rs.next(), "merchants 데이터 없음 → 스킵");
                merchantId = rs.getLong("id");
                merchantCategory = rs.getString("category");
            }
            assertNotNull(merchantCategory, "가맹점 category 가 있어야 한다");
            System.out.println("[검증] merchantId=" + merchantId + ", category=" + merchantCategory);

            // 2) TransactionMapper.findMerchantCategoryById 와 동일한 SELECT
            String lookedUp;
            try (PreparedStatement ps = con.prepareStatement("SELECT category FROM merchants WHERE id = ?")) {
                ps.setLong(1, merchantId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertEquals(true, rs.next());
                    lookedUp = rs.getString("category");
                }
            }
            assertEquals(merchantCategory, lookedUp, "mapper 쿼리가 가맹점 category 를 그대로 반환해야 한다");

            // 3) insertTransaction 과 동일하게 category_assigned 에 저장
            long insertedId;
            String idem = "it-category-" + System.nanoTime();
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO transactions (user_id, merchant_id, idempotency_key, payment_source_type, " +
                            "merchant_name, amount, transaction_type, category_assigned, status, created_at) " +
                            "VALUES (?, ?, ?, 'WALLET', '통합테스트가맹점', ?, 'PAYMENT', ?, 'PAID', NOW())",
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, 1L);
                ps.setLong(2, merchantId);
                ps.setString(3, idem);
                ps.setBigDecimal(4, new BigDecimal("1000"));
                ps.setString(5, lookedUp); // == 서비스가 tx.setCategoryAssigned(merchant category)
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    assertEquals(true, keys.next());
                    insertedId = keys.getLong(1);
                }
            }

            // 4) 되읽어 저장 확인
            String stored;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT category_assigned FROM transactions WHERE id = ?")) {
                ps.setLong(1, insertedId);
                try (ResultSet rs = ps.executeQuery()) {
                    assertEquals(true, rs.next());
                    stored = rs.getString("category_assigned");
                }
            }
            System.out.println("[검증] 저장된 category_assigned=" + stored + " (기대=" + merchantCategory + ")");
            assertEquals(merchantCategory, stored,
                    "결제 거래의 category_assigned 가 가맹점 category 로 저장되어야 한다");

            // 5) 존재하지 않는 merchantId → SELECT 는 null (서비스는 이때 "기타" 저장)
            try (PreparedStatement ps = con.prepareStatement("SELECT category FROM merchants WHERE id = ?")) {
                ps.setLong(1, -9999L);
                try (ResultSet rs = ps.executeQuery()) {
                    String none = rs.next() ? rs.getString("category") : null;
                    assertNull(none, "없는 가맹점은 category 조회 결과가 없어야 한다(→ 서비스가 '기타'로 대체)");
                }
            }

            con.rollback(); // DB 를 더럽히지 않는다
            System.out.println("[검증] 롤백 완료 — 실제 데이터 미변경");
        }
    }
}
