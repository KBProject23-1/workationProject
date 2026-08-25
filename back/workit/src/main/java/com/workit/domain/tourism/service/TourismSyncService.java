package com.workit.domain.tourism.service;

import com.workit.domain.tourism.client.TourApiClient;
import com.workit.domain.tourism.exception.TourismErrorCode;
import com.workit.domain.tourism.mapper.TourismMapper;
import com.workit.domain.tourism.vo.TourApiItem;
import com.workit.domain.tourism.vo.TourApiDetail;
import com.workit.domain.tourism.vo.TourApiPage;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.domain.tourism.vo.TourismRegionVO;
import com.workit.domain.tourism.vo.TourismSyncMode;
import com.workit.domain.tourism.vo.TourismSyncRunVO;
import com.workit.domain.tourism.vo.TourismTargetRegion;
import com.workit.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class TourismSyncService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter API_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final TourApiClient tourApiClient;
    private final TourismMapper tourismMapper;
    private final TourismMerchantPersistenceService persistenceService;
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    public TourismSyncRunVO sync(TourismSyncMode mode) {
        return sync(mode, null);
    }

    public TourismSyncRunVO sync(TourismSyncMode mode, TourismPlaceType targetPlaceType) {
        if (!tourApiClient.isConfigured()) {
            throw new BusinessException(TourismErrorCode.API_KEY_NOT_CONFIGURED);
        }
        if (!syncing.compareAndSet(false, true)) {
            throw new BusinessException(TourismErrorCode.SYNC_IN_PROGRESS);
        }

        TourismSyncMode safeMode = mode == null ? TourismSyncMode.INCREMENTAL : mode;
        LocalDate today = LocalDate.now(SEOUL);
        List<String> dates = safeMode == TourismSyncMode.FULL
                ? Collections.emptyList()
                : Arrays.asList(today.minusDays(1).format(API_DATE), today.format(API_DATE));
        TourismSyncRunVO run = new TourismSyncRunVO();
        run.setMode(safeMode.name());
        run.setStatus("RUNNING");
        run.setTargetDates(dates.isEmpty() ? null : String.join(",", dates));

        int processed = 0;
        int deactivated = 0;
        try {
            tourismMapper.insertSyncRun(run);
            List<TourismRegionVO> regions = tourismMapper.selectRegions();
            validateTargetRegions(regions);
            if (safeMode == TourismSyncMode.FULL) {
                if (includesActivities(targetPlaceType)) {
                    for (TourismCategory category : TourismCategory.values()) {
                        processed += fetchAll(category, TourismTargetRegion.JEJU, true, null,
                                run.getId(), regions);
                        deactivated += tourismMapper.deactivateMissing(category, run.getId());
                    }
                }
                for (TourismPlaceType placeType : requestedPlaceTypes(targetPlaceType)) {
                    processed += fetchAll(placeType, TourismTargetRegion.JEJU, true,
                            null, run.getId(), regions);
                    deactivated += tourismMapper.deactivateMissingPlaceType(
                            placeType, run.getId());
                }
            } else {
                for (String date : dates) {
                    if (includesActivities(targetPlaceType)) {
                        for (TourismCategory category : TourismCategory.values()) {
                            processed += fetchAll(category, TourismTargetRegion.JEJU,
                                    true, date, null, regions);
                            processed += fetchAll(category, TourismTargetRegion.JEJU,
                                    false, date, null, regions);
                        }
                    }
                    for (TourismPlaceType placeType : requestedPlaceTypes(targetPlaceType)) {
                        processed += fetchAll(placeType, TourismTargetRegion.JEJU,
                                true, date, null, regions);
                        processed += fetchAll(placeType, TourismTargetRegion.JEJU,
                                false, date, null, regions);
                    }
                }
            }
            tourismMapper.completeSyncRun(run.getId(), "SUCCESS", processed, deactivated, null);
            return tourismMapper.selectSyncRun(run.getId());
        } catch (RuntimeException e) {
            if (run.getId() != null) {
                tourismMapper.completeSyncRun(run.getId(), "FAILED", processed, deactivated,
                        abbreviate(e.getMessage()));
            }
            throw e;
        } finally {
            syncing.set(false);
        }
    }

    public TourismSyncRunVO latest() {
        return tourismMapper.selectLatestSyncRun();
    }

    private int fetchAll(TourismCategory category, TourismTargetRegion targetRegion,
                         boolean active, String date, Long syncId,
                         List<TourismRegionVO> regions) {
        int pageNo = 1;
        int processed = 0;
        int returned = 0;
        while (true) {
            TourApiPage page = tourApiClient.fetch(category, targetRegion, pageNo,
                    active, date, syncId);
            if (page.getItems().isEmpty() && page.getTotalCount() > 0) {
                if (returned >= page.getTotalCount()) {
                    return processed;
                }
                throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                        "전체 건수와 일치하지 않는 빈 페이지입니다. category="
                                + category + ", region=" + targetRegion
                                + ", page=" + pageNo + ", returned=" + returned
                                + ", total=" + page.getTotalCount());
            }
            for (TourApiItem item : page.getItems()) {
                enrichDescription(item);
                if (persistenceService.save(item, resolveRegionId(targetRegion, regions))) {
                    processed++;
                }
            }
            returned += Math.max(page.getNumOfRows(), page.getItems().size());
            if (page.getItems().isEmpty()
                    || returned >= page.getTotalCount()) {
                return processed;
            }
            pageNo++;
        }
    }

    private int fetchAll(TourismPlaceType placeType, TourismTargetRegion targetRegion,
                         boolean active, String date, Long syncId,
                         List<TourismRegionVO> regions) {
        int pageNo = 1;
        int processed = 0;
        int returned = 0;
        while (true) {
            TourApiPage page = tourApiClient.fetch(placeType, targetRegion, pageNo,
                    active, date, syncId);
            if (page.getItems().isEmpty() && page.getTotalCount() > 0) {
                if (returned >= page.getTotalCount()) return processed;
                throw new BusinessException(TourismErrorCode.TOUR_API_ERROR,
                        "전체 건수와 일치하지 않는 빈 페이지입니다. placeType="
                                + placeType + ", region=" + targetRegion
                                + ", page=" + pageNo + ", returned=" + returned
                                + ", total=" + page.getTotalCount());
            }
            for (TourApiItem item : page.getItems()) {
                enrichDescription(item);
                if (persistenceService.save(item, resolveRegionId(targetRegion, regions))) {
                    processed++;
                }
            }
            returned += Math.max(page.getNumOfRows(), page.getItems().size());
            if (page.getItems().isEmpty() || returned >= page.getTotalCount()) {
                return processed;
            }
            pageNo++;
        }
    }

    private void enrichDescription(TourApiItem item) {
        if (!item.isActive()) {
            return;
        }
        if (item.getLatitude() == null || item.getLongitude() == null
                || (!StringUtils.hasText(item.getAddress1())
                && !StringUtils.hasText(item.getAddress2()))) {
            return;
        }
        if (!tourismMapper.shouldFetchDescription(
                item.getContentId(), item.getModifiedTime())) {
            return;
        }
        try {
            int contentTypeId = item.getContentTypeId() > 0
                    ? item.getContentTypeId() : item.getCategory().getContentTypeId();
            TourApiDetail detail = tourApiClient.fetchDetail(
                    item.getContentId(), contentTypeId);
            item.setDescription(detail.getDescription());
            if (StringUtils.hasText(detail.getPhoneNumber())) {
                item.setPhoneNumber(detail.getPhoneNumber());
            }
            item.setCheckInTime(detail.getCheckInTime());
            item.setCheckOutTime(detail.getCheckOutTime());
            item.setDetailFetched(true);
        } catch (RuntimeException e) {
            log.warn("TourAPI 상세정보 조회 실패. contentId={}, placeType={}, category={}, message={}",
                    item.getContentId(), item.getPlaceType(), item.getCategory(), e.getMessage());
        }
    }

    private List<TourismPlaceType> jejuPlaceTypes() {
        return Arrays.asList(TourismPlaceType.RESTAURANT,
                TourismPlaceType.ACCOMMODATION);
    }

    private boolean includesActivities(TourismPlaceType targetPlaceType) {
        return targetPlaceType == null || targetPlaceType == TourismPlaceType.ACTIVITY;
    }

    private List<TourismPlaceType> requestedPlaceTypes(TourismPlaceType targetPlaceType) {
        if (targetPlaceType == null) {
            return jejuPlaceTypes();
        }
        if (targetPlaceType == TourismPlaceType.ACTIVITY) {
            return Collections.emptyList();
        }
        return Collections.singletonList(targetPlaceType);
    }

    private Long resolveRegionId(TourismTargetRegion targetRegion,
                                 List<TourismRegionVO> regions) {
        for (TourismRegionVO region : regions) {
            if (targetRegion.getRegionName().equals(region.getName())) {
                return region.getId();
            }
        }
        throw new IllegalStateException("region 테이블에 대상 지역이 없습니다: "
                + targetRegion.getRegionName());
    }

    private void validateTargetRegions(List<TourismRegionVO> regions) {
        resolveRegionId(TourismTargetRegion.JEJU, regions);
    }

    private String abbreviate(String value) {
        if (value == null || value.length() <= 1000) return value;
        return value.substring(0, 1000);
    }
}
