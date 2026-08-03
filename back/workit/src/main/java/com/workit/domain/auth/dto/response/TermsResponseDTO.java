package com.workit.domain.auth.dto.response;

import com.workit.domain.auth.vo.TermsVO;
import lombok.Builder;
import lombok.Getter;

// 약관 1건 응답
// API 스펙: termId, title, content, required
@Getter
@Builder
public class TermsResponseDTO {

    private Long termId;
    private String title;
    private String content;
    private Boolean required;

    public static TermsResponseDTO from(TermsVO vo) {
        return TermsResponseDTO.builder()
                .termId(vo.getId())
                .title(vo.getTitle())
                .content(vo.getContent())
                .required(vo.getRequired())
                .build();
    }
}
