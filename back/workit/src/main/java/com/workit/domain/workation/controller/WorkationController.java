package com.workit.domain.workation.controller;

import com.workit.domain.workation.dto.request.WorkationCreateRequestDTO;
import com.workit.domain.workation.dto.request.WorkationUpdateRequestDTO;
import com.workit.domain.workation.dto.response.*;
import com.workit.domain.workation.service.WorkationService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.dto.PageResponseDTO;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workations")
@RequiredArgsConstructor
@Slf4j
public class WorkationController {

    private final WorkationService workationService;

    // 1.1 워케이션 등록 폼
    @PostMapping
    public ResponseEntity<CommonResponse<WorkationResponseDTO>> workationAdd(
            @RequestBody WorkationCreateRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.created(workationService.addWorkation(userId, dto));
    }

    // 1.2 진행 중 워케이션 조회
    @GetMapping("/current")
    public ResponseEntity<CommonResponse<WorkationCurrentResponseDTO>> workationCurrentGet() {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(workationService.getCurrentWorkation(userId));
    }

    // 1.3 워케이션 기록 목록
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponseDTO<WorkationHistoryResponseDTO>>> workationListGet(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(workationService.getWorkationHistory(userId, page, size));
    }

    // 1.4 워케이션 수정
    @PutMapping("/{workationId}")
    public ResponseEntity<CommonResponse<WorkationResponseDTO>> workationModify(
            @PathVariable("workationId") Long workationId,
            @RequestBody WorkationUpdateRequestDTO dto) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(workationService.modifyWorkation(userId, workationId, dto));
    }

    // 1.5 워케이션 삭제
    @DeleteMapping("/{workationId}")
    public ResponseEntity<Void> workationRemove(
            @PathVariable("workationId") Long workationId) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        workationService.removeWorkation(userId, workationId);
        return GlobalResponseFactory.noContent();
    }

    // 1.6 워케이션 종료
    @PatchMapping("/{workationId}/settle")
    public ResponseEntity<CommonResponse<WorkationSettleResponseDTO>> workationSettle(
            @PathVariable("workationId") Long workationId) {

        // JWT 토큰에서 userId 추출로 교체 필요(추후 삭제)
        Long userId = 1L;

        return GlobalResponseFactory.success(workationService.settleWorkation(userId, workationId));
    }
}
