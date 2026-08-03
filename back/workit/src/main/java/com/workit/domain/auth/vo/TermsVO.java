package com.workit.domain.auth.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

// 약관 마스터 테이블(terms) 매핑 VO
// ERD 기준 컬럼: id, title, content, required, created_at
@Getter
@Setter
@ToString
public class TermsVO {

    /** 약관 고유 번호(PK) */
    private Long id;

    /** 약관 제목 */
    private String title;

    /** 약관 본문 상세 내용 */
    private String content;

    /** 필수 여부 (0:선택, 1:필수) */
    private Boolean required;

    /** 약관 등록 일시 */
    private LocalDateTime createdAt;
}
