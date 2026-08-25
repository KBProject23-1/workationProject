package com.workit.domain.tourism.controller;

import com.workit.domain.tourism.service.TourismSyncService;
import com.workit.domain.tourism.vo.TourismCategory;
import com.workit.domain.tourism.vo.TourismSyncMode;
import com.workit.domain.tourism.vo.TourismSyncRunVO;
import com.workit.domain.tourism.vo.TourismPlaceType;
import com.workit.global.dto.CommonResponse;
import com.workit.global.response.GlobalResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tourism")
@RequiredArgsConstructor
public class TourismController {
    private final TourismSyncService tourismSyncService;

    @GetMapping("/categories")
    public ResponseEntity<CommonResponse<List<TourismCategory>>> categories() {
        return GlobalResponseFactory.success(Arrays.asList(TourismCategory.values()));
    }

    @GetMapping("/place-types")
    public ResponseEntity<CommonResponse<List<TourismPlaceType>>> placeTypes() {
        return GlobalResponseFactory.success(Arrays.asList(TourismPlaceType.values()));
    }

    @GetMapping("/sync/status")
    public ResponseEntity<CommonResponse<TourismSyncRunVO>> status() {
        return GlobalResponseFactory.success(tourismSyncService.latest());
    }

    @PostMapping("/sync")
    public ResponseEntity<CommonResponse<TourismSyncRunVO>> sync(
            @RequestParam(value = "mode", defaultValue = "INCREMENTAL") TourismSyncMode mode) {
        return GlobalResponseFactory.success(
                tourismSyncService.sync(mode), "관광 데이터 동기화가 완료되었습니다.");
    }

    @PostMapping("/sync/activities")
    public ResponseEntity<CommonResponse<TourismSyncRunVO>> syncActivities(
            @RequestParam(value = "mode", defaultValue = "INCREMENTAL") TourismSyncMode mode) {
        return syncByPlaceType(mode, TourismPlaceType.ACTIVITY, "여가");
    }

    @PostMapping("/sync/restaurants")
    public ResponseEntity<CommonResponse<TourismSyncRunVO>> syncRestaurants(
            @RequestParam(value = "mode", defaultValue = "INCREMENTAL") TourismSyncMode mode) {
        return syncByPlaceType(mode, TourismPlaceType.RESTAURANT, "음식점");
    }

    @PostMapping("/sync/accommodations")
    public ResponseEntity<CommonResponse<TourismSyncRunVO>> syncAccommodations(
            @RequestParam(value = "mode", defaultValue = "INCREMENTAL") TourismSyncMode mode) {
        return syncByPlaceType(mode, TourismPlaceType.ACCOMMODATION, "숙소");
    }

    private ResponseEntity<CommonResponse<TourismSyncRunVO>> syncByPlaceType(
            TourismSyncMode mode, TourismPlaceType placeType, String label) {
        return GlobalResponseFactory.success(
                tourismSyncService.sync(mode, placeType),
                label + " 데이터 동기화가 완료되었습니다.");
    }
}
