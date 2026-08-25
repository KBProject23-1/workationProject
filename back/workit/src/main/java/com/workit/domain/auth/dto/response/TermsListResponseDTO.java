package com.workit.domain.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 약관 목록 응답
// API 스펙: data 안에 termsList 배열로 반환
@Getter
@Builder
public class TermsListResponseDTO {

    private List<TermsResponseDTO> termsList;

    public static TermsListResponseDTO of(List<TermsResponseDTO> termsList) {
        return TermsListResponseDTO.builder()
                .termsList(termsList)
                .build();
    }
}
