package com.workit.domain.tourism.scheduler;

import com.workit.domain.tourism.service.TourismSyncService;
import com.workit.domain.tourism.vo.TourismSyncMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TourismSyncScheduler {
    private final TourismSyncService tourismSyncService;

    @Value("${tourism.sync.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${tourism.sync.cron:0 0 3 * * *}", zone = "${tourism.sync.zone:Asia/Seoul}")
    public void syncDaily() {
        if (!enabled) return;
        try {
            tourismSyncService.sync(TourismSyncMode.INCREMENTAL);
        } catch (Exception e) {
            log.error("TourAPI 일일 동기화 실패", e);
        }
    }
}
