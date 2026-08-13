package com.workit.domain.schedule.controller;

import com.workit.domain.schedule.dto.request.ScheduleCreateRequestDTO;
import com.workit.domain.schedule.dto.request.ScheduleUpdateRequestDTO;
import com.workit.domain.schedule.dto.response.ScheduleDetailResponseDTO;
import com.workit.domain.schedule.dto.response.ScheduleListResponseDTO;
import com.workit.domain.schedule.service.ScheduleService;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import com.workit.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// 워케이션 스케줄러.
// 숙소·공유오피스 예약과 음식점·여가 방문 계획을 합쳐 날짜별로 보여준다
@RestController
@RequestMapping("/api/v1/workations/{workationId}/schedules")
@RequiredArgsConstructor
@Slf4j
public class ScheduleController {

    private final ScheduleService scheduleService;

    // 통합 조회
    // startDate 를 생략하면 오늘, days 를 생략하면 2일치를 준다
    @GetMapping
    public ResponseEntity<CommonResponse<ScheduleListResponseDTO>> scheduleListGet(
            @CurrentUser Long userId,
            @PathVariable("workationId") Long workationId,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(value = "days", required = false) Integer days) {

        return GlobalResponseFactory.success(
                scheduleService.getScheduleList(userId, workationId, startDate, days));
    }

    // 음식점·여가 활동 방문 계획 등록
    @PostMapping
    public ResponseEntity<CommonResponse<ScheduleDetailResponseDTO>> scheduleAdd(
            @CurrentUser Long userId,
            @PathVariable("workationId") Long workationId,
            @RequestBody ScheduleCreateRequestDTO dto) {

        return GlobalResponseFactory.created(
                scheduleService.addSchedule(userId, workationId, dto));
    }

    // 일정 상세. 스케줄러 카드에서 눌러 들어온다
    @GetMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleDetailResponseDTO>> scheduleDetailGet(
            @CurrentUser Long userId,
            @PathVariable("workationId") Long workationId,
            @PathVariable("scheduleId") Long scheduleId) {

        return GlobalResponseFactory.success(scheduleService.getSchedule(userId, scheduleId));
    }

    // 일정 시각 수정. 가맹점은 바꾸지 않는다
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<CommonResponse<ScheduleDetailResponseDTO>> scheduleModify(
            @CurrentUser Long userId,
            @PathVariable("workationId") Long workationId,
            @PathVariable("scheduleId") Long scheduleId,
            @RequestBody ScheduleUpdateRequestDTO dto) {

        return GlobalResponseFactory.success(
                scheduleService.modifySchedule(userId, workationId, scheduleId, dto));
    }

    // 일정 삭제. 예약과 달리 결제가 없어 상태 전이 없이 바로 지운다
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> scheduleRemove(
            @CurrentUser Long userId,
            @PathVariable("workationId") Long workationId,
            @PathVariable("scheduleId") Long scheduleId) {

        scheduleService.removeSchedule(userId, workationId, scheduleId);
        return GlobalResponseFactory.noContent();
    }
}
