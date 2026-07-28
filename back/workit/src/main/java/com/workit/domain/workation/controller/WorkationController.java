package com.workit.domain.workation.controller;

import com.workit.domain.workation.dto.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.WorkationCurrentResponseDTO;
import com.workit.domain.workation.dto.WorkationResponseDTO;
import com.workit.domain.workation.service.WorkationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workations")
@RequiredArgsConstructor
@Log4j2
public class WorkationController {

    private final WorkationService workationService;

    /** 1.1 워케이션 등록 폼 */
    @PostMapping
    public ResponseEntity<WorkationResponseDTO> workationAdd(
            @RequestBody WorkationCreateRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workationService.addWorkation(userId, dto));
    }

    /** 1.2 진행 중 워케이션 조회 */
    @GetMapping("/current")
    public ResponseEntity<WorkationCurrentResponseDTO> workationCurrentGet() {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return ResponseEntity.ok(workationService.getCurrentWorkation(userId));
    }
}
