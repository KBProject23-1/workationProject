package com.workit.domain.auth.service;

import com.workit.domain.auth.dto.response.IdentityVerificationResponseDTO;
import com.workit.domain.auth.dto.response.TermsListResponseDTO;
import com.workit.domain.auth.dto.response.TermsResponseDTO;
import com.workit.domain.auth.mapper.AuthMapper;
import com.workit.domain.auth.provider.IdentityVerificationProvider;
import com.workit.domain.auth.provider.IdentityVerificationResult;
import com.workit.global.util.PersonalDataCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final IdentityVerificationProvider identityVerificationProvider;

    @Override
    @Transactional(readOnly = true)
    public TermsListResponseDTO getTermsList() {

        List<TermsResponseDTO> terms = authMapper.selectTermsList()
                .stream()
                .map(TermsResponseDTO::from)
                .collect(Collectors.toList());

        return TermsListResponseDTO.of(terms);
    }

    @Override
    public IdentityVerificationResponseDTO verifyIdentity(String identityVerificationId) {

        // 1. Provider 로 PASS 인증 결과 검증 (현재 Mock — 실패 시 BusinessException)
        IdentityVerificationResult result = identityVerificationProvider.verify(identityVerificationId);

        // 2. CI AES-256 암호화 (knowledge.md: 암호화는 반드시 Service Layer 에서)
        String encryptedCi = PersonalDataCipher.encrypt(result.getCi());

        // 3. 응답 생성 (identityToken 은 회원가입 플로우에서 JWT 발급으로 교체 예정)
        return IdentityVerificationResponseDTO.of(encryptedCi, result.getName());
    }
}
