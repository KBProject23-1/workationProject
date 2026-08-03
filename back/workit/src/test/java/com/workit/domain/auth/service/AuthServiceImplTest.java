package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.vo.TermsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// AuthServiceImpl 단위 테스트
// - Mockito 의존성이 없으므로 AuthMapper 를 수동 Fake 로 구현해 주입한다
// - API 응답 구조(CommonResponse, data.termsList)는 Controller 계층에서 확인하도록 유지
class AuthServiceImplTest {

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
        authService = new AuthServiceImpl(new FakeAuthMapper(terms));
    }

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
        AuthService emptyService = new AuthServiceImpl(new FakeAuthMapper(Collections.emptyList()));

        TermsListResponseDTO result = emptyService.getTermsList();

        assertNotNull(result);
        assertNotNull(result.getTermsList());
        assertTrue(result.getTermsList().isEmpty());
    }
}
