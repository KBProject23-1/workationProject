package com.workit.domain.tourism.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TourismSyncRunVO {
    private Long id;
    private String mode;
    private String status;
    private String targetDates;
    private int processedCount;
    private int deactivatedCount;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
