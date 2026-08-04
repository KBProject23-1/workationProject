package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.MockIdentityVerificationProvider;
import com.workit.domain.auth.vo.TermsVO;
import com.workit.exception.BusinessException;
import com.workit.global.util.PersonalDataCipher;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// AuthServiceImpl 단위 테스트
// - Mockito 의존성이 없으므로 AuthMapper 를 수동 Fake 로, Provider 는 Mock 구현체로 주입한다
// - API 응답 구조(CommonResponse, data.termsList)는 Controller 계층에서 확인하도록 유지
class AuthServiceImplTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef"; // 32바이트

    private AuthService authService;

    // 수동 Fake Mapper - 테스트에서 원하는 약관 목록을 그대로 돌려준다
    private static class FakeAuthMapper implements AuthMapper {

        private final List<TermsVO> terms;

        FakeAuthMapper(List<TermsVO> terms) {
            this.terms = terms;
        }

        @Override
        public List<TermsVO> selectTermsList() {
            return terms;
        }
    }

    @BeforeAll
    static void setUpAesKey() {
        // verifyIdentity 가 내부에서 PersonalDataCipher.encrypt() 를 호출하므로
        // 실행 환경(AES 환경변수)과 무관하게 동작하도록 키를 주입한다
        System.setProperty("personal.data.aes.key", TEST_AES_KEY);
        PersonalDataCipher.reloadKey();
    }

    @AfterAll
    static void tearDownAesKey() {
        // 다른 테스트 클래스에 영향이 없도록 원상 복구
        System.clearProperty("personal.data.aes.key");
        PersonalDataCipher.reloadKey();
    }

    private TermsVO term(Long id, String title, boolean required) {
        TermsVO vo = new TermsVO();
        vo.setId(id);
        vo.setTitle(title);
        vo.setContent("제1조: " + title + " 본문 내용");
        vo.setRequired(required);
        vo.setCreatedAt(LocalDateTime.of(2026, 7, 30, 0, 0));
        return vo;
    }

    @BeforeEach
    void setUp() {
        List<TermsVO> terms = Arrays.asList(
                term(1L, "서비스 이용약관", true),
                term(2L, "개인정보 수집 및 이용 동의", true),
                term(3L, "마케팅 정보 수신 동의", false)
        );
        authService = new AuthServiceImpl(
                new FakeAuthMapper(terms),
                new MockIdentityVerificationProvider()
        );
    }

    // ---------- 약관 목록 조회 ----------

    @Test
    @DisplayName("정상 약관 목록 조회 - 전체 약관을 DTO로 변환해 반환")
    void getTermsList_success() {
        TermsListResponseDTO result = authService.getTermsList();

        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertEquals(3, result.getTermsList().size());
    }

    @Test
    @DisplayName("Mapper 조회 결과가 Service 반환값에 그대로 반영되는지 검증")
    void getTermsList_mapsMapperResult() {
        TermsListResponseDTO result = authService.getTermsList();

        TermsResponseDTO first = result.getTermsList().get(0);
        assertEquals(1L, first.getTermId());
        assertEquals("서비스 이용약관", first.getTitle());
        assertEquals("제1조: 서비스 이용약관 본문 내용", first.getContent());
        assertTrue(first.getRequired());

        TermsResponseDTO optional = result.getTermsList().get(2);
        assertEquals(3L, optional.getTermId());
        assertEquals("마케팅 정보 수신 동의", optional.getTitle());
        assertFalse(optional.getRequired());
    }

    @Test
    @DisplayName("데이터 없음 처리 - 빈 목록을 반환")
    void getTermsList_emptyList() {
        AuthService emptyService = new AuthServiceImpl(
                new FakeAuthMapper(Collections.emptyList()),
                new MockIdentityVerificationProvider()
        );

        TermsListResponseDTO result = emptyService.getTermsList();

        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertTrue(result.getTermsList().isEmpty());
    }

    // ---------- 본인인증 검증 ----------

    @Test
    @DisplayName("본인인증 성공 - CI가 AES 암호화된 identityToken과 name을 반환")
    void verifyIdentity_success() {
        IdentityVerificationResponseDTO result =
                authService.verifyIdentity("imp_ver_1234567890");

        assertNotNull(result);
        assertEquals("홍길동", result.getName());
        assertNotNull(result.getIdentityToken());
        assertFalse(result.getIdentityToken().isEmpty());

        // CI 평문이 응답에 노출되지 않아야 한다
        assertFalse(result.getIdentityToken().contains("MOCK-CI-"));

        // identityToken 을 복호화하면 Provider 가 반환한 원본 CI 가 복원된다
        assertEquals("MOCK-CI-imp_ver_1234567890",
                PersonalDataCipher.decrypt(result.getIdentityToken()));
    }

    @Test
    @DisplayName("본인인증 실패 - 유효하지 않은 인증 ID는 예외 발생")
    void verifyIdentity_invalidId_throws() {
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(MockIdentityVerificationProvider.INVALID_IDENTIFIER));
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(""));
        assertThrows(BusinessException.class,
                () -> authService.verifyIdentity(null));
    }
}
