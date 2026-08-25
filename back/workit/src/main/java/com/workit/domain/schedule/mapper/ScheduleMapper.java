package com.workit.domain.schedule.mapper;

import com.workit.domain.schedule.vo.ScheduleItemVO;
import com.workit.domain.schedule.vo.ScheduleVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleMapper {

    int insertSchedule(ScheduleVO vo);

    // 소유자 조건이 SQL 에 있어 남의 일정은 조회되지 않는다. 없으면 null
    ScheduleVO selectScheduleById(@Param("scheduleId") Long scheduleId,
                                  @Param("userId") Long userId);

    int updateSchedule(@Param("scheduleId") Long scheduleId,
                       @Param("workationId") Long workationId,
                       @Param("userId") Long userId,
                       @Param("scheduledAt") LocalDateTime scheduledAt);

    int deleteSchedule(@Param("scheduleId") Long scheduleId,
                       @Param("workationId") Long workationId,
                       @Param("userId") Long userId);

    // 가맹점 업종. 없으면 null
    String selectMerchantCategory(@Param("merchantId") Long merchantId);

    // 스케줄러 통합 조회. 예약과 일정을 합쳐 날짜 순으로 반환한다
    List<ScheduleItemVO> selectScheduleItems(@Param("workationId") Long workationId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("days") int days);
}
