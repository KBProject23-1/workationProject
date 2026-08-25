package com.workit.domain.schedule.service;

import com.workit.domain.schedule.dto.request.ScheduleCreateRequestDTO;
import com.workit.domain.schedule.dto.request.ScheduleUpdateRequestDTO;
import com.workit.domain.schedule.dto.response.ScheduleDayResponseDTO;
import com.workit.domain.schedule.dto.response.ScheduleDetailResponseDTO;
import com.workit.domain.schedule.dto.response.ScheduleItemResponseDTO;
import com.workit.domain.schedule.dto.response.ScheduleListResponseDTO;
import com.workit.domain.schedule.exception.ScheduleErrorCode;
import com.workit.domain.schedule.mapper.ScheduleMapper;
import com.workit.domain.schedule.vo.ScheduleItemVO;
import com.workit.domain.schedule.vo.ScheduleVO;
import com.workit.domain.workation.service.WorkationOwnershipValidator;
import com.workit.domain.workation.vo.WorkationVO;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    // 화면은 기본 2일, 토글하면 7일을 쓴다.
    // 값을 못 박지 않고 범위로 받아 두면 화면이 3일치를 쓰게 될 때 서버를 고치지 않아도 된다
    private static final int DEFAULT_DAYS = 2;
    private static final int MAX_DAYS = 7;

    // 방문 계획으로 넣을 수 있는 가맹점 업종.
    // 숙소와 공유오피스는 예약이라 reservations 에 들어간다
    private static final Set<String> SCHEDULABLE_CATEGORIES = Set.of("RESTAURANT", "ACTIVITY");

    private final ScheduleMapper scheduleMapper;
    private final WorkationOwnershipValidator ownershipValidator;

    // =====================================================================================
    // 일정 등록
    // =====================================================================================

    @Override
    @Transactional
    public ScheduleDetailResponseDTO addSchedule(Long userId, Long workationId,
                                                 ScheduleCreateRequestDTO dto) {

        WorkationVO workation = ownershipValidator.getOwnedActive(userId, workationId, "일정을 등록");

        if (dto.getScheduledAt() == null) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULED_AT_REQUIRED);
        }
        validateWithinPeriod(dto.getScheduledAt().toLocalDate(), workation);
        validateSchedulable(dto.getMerchantId());

        ScheduleVO vo = new ScheduleVO();
        vo.setWorkationId(workationId);
        vo.setMerchantId(dto.getMerchantId());
        vo.setScheduledAt(dto.getScheduledAt());

        scheduleMapper.insertSchedule(vo);
        log.info("일정 등록 - workationId: {}, merchantId: {}, scheduledAt: {}",
                workationId, dto.getMerchantId(), dto.getScheduledAt());

        return getSchedule(userId, vo.getId());
    }

    // =====================================================================================
    // 스케줄러 통합 조회
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public ScheduleListResponseDTO getScheduleList(Long userId, Long workationId,
                                                   LocalDate startDate, Integer days) {

        WorkationVO workation = ownershipValidator.getOwned(userId, workationId);

        int safeDays = (days == null) ? DEFAULT_DAYS : days;
        if (safeDays < 1 || safeDays > MAX_DAYS) {
            throw new BusinessException(ScheduleErrorCode.DAYS_OUT_OF_RANGE);
        }

        LocalDate today = LocalDate.now(SEOUL);
        LocalDate from = (startDate == null) ? today : startDate;

        List<ScheduleItemVO> items =
                scheduleMapper.selectScheduleItems(workationId, from, safeDays);

        // 조회한 날짜를 모두 만들어 두고 그 위에 항목을 얹는다.
        // 일정이 없는 날도 빈 배열로 내려가야 화면이 "일정이 없어요" 를 그릴 수 있다
        Map<LocalDate, List<ScheduleItemVO>> grouped = items.stream()
                .collect(Collectors.groupingBy(ScheduleItemVO::getItemDate));

        List<ScheduleDayResponseDTO> schedules = new ArrayList<>();
        for (int offset = 0; offset < safeDays; offset++) {
            LocalDate date = from.plusDays(offset);
            schedules.add(ScheduleDayResponseDTO.builder()
                    .date(date)
                    .isToday(date.equals(today))
                    .items(grouped.getOrDefault(date, List.of()).stream()
                            .map(ScheduleItemResponseDTO::from)
                            .collect(Collectors.toList()))
                    .build());
        }

        return ScheduleListResponseDTO.builder()
                .workationId(workationId)
                .workationStartDate(workation.getStartDate())
                .workationEndDate(workation.getEndDate())
                .startDate(from)
                .days(safeDays)
                .schedules(schedules)
                .build();
    }

    // =====================================================================================
    // 일정 상세
    // =====================================================================================

    @Override
    @Transactional(readOnly = true)
    public ScheduleDetailResponseDTO getSchedule(Long userId, Long scheduleId) {

        ScheduleVO vo = scheduleMapper.selectScheduleById(scheduleId, userId);

        // 소유자 조건이 SQL 에 있어 남의 일정도 여기서 걸린다
        if (vo == null) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        }
        return ScheduleDetailResponseDTO.from(vo);
    }

    // =====================================================================================
    // 일정 시각 수정
    // =====================================================================================

    @Override
    @Transactional
    public ScheduleDetailResponseDTO modifySchedule(Long userId, Long workationId, Long scheduleId,
                                                    ScheduleUpdateRequestDTO dto) {

        WorkationVO workation = ownershipValidator.getOwnedActive(userId, workationId, "일정을 수정");

        if (dto.getScheduledAt() == null) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULED_AT_REQUIRED);
        }
        validateWithinPeriod(dto.getScheduledAt().toLocalDate(), workation);

        // 존재 여부를 UPDATE 결과로 판정하면 안 된다.
        // 같은 시각으로 다시 저장하면 MySQL 이 0행을 반환해 없는 일정으로 오인한다
        ScheduleVO existing = scheduleMapper.selectScheduleById(scheduleId, userId);

        if (existing == null || !workationId.equals(existing.getWorkationId())) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        }

        scheduleMapper.updateSchedule(scheduleId, workationId, userId, dto.getScheduledAt());
        log.info("일정 수정 - workationId: {}, scheduleId: {}, scheduledAt: {}",
                workationId, scheduleId, dto.getScheduledAt());

        return getSchedule(userId, scheduleId);
    }

    // =====================================================================================
    // 일정 삭제
    // =====================================================================================

    // 예약과 달리 결제가 없어 상태 전이 없이 바로 지운다
    @Override
    @Transactional
    public void removeSchedule(Long userId, Long workationId, Long scheduleId) {

        ownershipValidator.getOwnedActive(userId, workationId, "일정을 삭제");

        int deleted = scheduleMapper.deleteSchedule(scheduleId, workationId, userId);

        if (deleted == 0) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULE_NOT_FOUND);
        }
        log.info("일정 삭제 - workationId: {}, scheduleId: {}", workationId, scheduleId);
    }

    // =====================================================================================
    // 검증
    // =====================================================================================

    private void validateWithinPeriod(LocalDate date, WorkationVO workation) {

        if (date.isBefore(workation.getStartDate()) || date.isAfter(workation.getEndDate())) {
            throw new BusinessException(ScheduleErrorCode.SCHEDULED_AT_OUT_OF_PERIOD);
        }
    }

    private void validateSchedulable(Long merchantId) {

        if (merchantId == null) {
            throw new BusinessException(ScheduleErrorCode.MERCHANT_NOT_FOUND);
        }

        String category = scheduleMapper.selectMerchantCategory(merchantId);

        if (category == null) {
            throw new BusinessException(ScheduleErrorCode.MERCHANT_NOT_FOUND);
        }
        if (!SCHEDULABLE_CATEGORIES.contains(category)) {
            throw new BusinessException(ScheduleErrorCode.MERCHANT_CATEGORY_INVALID);
        }
    }
}
