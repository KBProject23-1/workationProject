package com.workit.domain.reservation.service;

import com.workit.domain.reservation.dto.request.ReservationCreateRequestDTO;
import com.workit.domain.reservation.dto.request.ReservationProductAvailabilityRequestDTO;
import com.workit.domain.reservation.dto.response.ReservationCancellationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCancelResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationCreateResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationDetailResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationListItemResponseDTO;
import com.workit.domain.reservation.dto.response.ReservationProductAvailabilityResponseDTO;
import com.workit.domain.reservation.exception.ReservationErrorCode;
import com.workit.domain.reservation.mapper.ReservationMapper;
import com.workit.domain.reservation.vo.ReservationCategory;
import com.workit.domain.reservation.vo.ReservationCancelInventoryVO;
import com.workit.domain.reservation.vo.ReservationCancelTargetVO;
import com.workit.domain.reservation.vo.ReservationCancelVO;
import com.workit.domain.reservation.vo.ReservationCancellationDetailVO;
import com.workit.domain.reservation.vo.ReservationCreateProductVO;
import com.workit.domain.reservation.vo.ReservationCreateVO;
import com.workit.domain.reservation.vo.ReservationCreateWorkationVO;
import com.workit.domain.reservation.vo.ReservationDailyInventoryVO;
import com.workit.domain.reservation.vo.ReservationDetailVO;
import com.workit.domain.reservation.vo.ReservationProductDetailType;
import com.workit.domain.reservation.vo.ReservationReviewAction;
import com.workit.domain.reservation.vo.ReservationStatus;
import com.workit.domain.transaction.dto.request.PaymentRequest;
import com.workit.domain.transaction.dto.response.CancelResponse;
import com.workit.domain.payment.service.PaymentService;
import com.workit.exception.BusinessException;
import com.workit.global.dto.PageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


// 예약 목록과 상세 조회에 필요한 검증 및 화면 상태 계산을 수행하는 서비스

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final long MAX_RESERVATION_SEQUENCE = 999999L;
    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter RESERVATION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReservationMapper reservationMapper;
    private final PaymentService paymentService;

    // 프론트 기기 식별자 연동 전 로컬 예약 결제 테스트용 임시값
    @Value("${reservation.dev-default-device-id:}")
    private String devDefaultDeviceId;


//  예약 상태가 CONFIRMED인 예약 중 이용이 끝난 예약을 COMPLETED로 변경
    @Override
    @Transactional
    public int modifyCompletedReservationStatuses(LocalDate today) {
        return reservationMapper.updateCompletedReservationStatuses(today);
    }


//    예약 생성, 결제, 재고 차감을 하나의 트랜잭션으로 묶음
    @Override
    @Transactional
    public ReservationCreateResponseDTO addReservation(
            Long userId,
            ReservationCreateRequestDTO request) {

        validateCreateRequest(userId, request);

        ReservationCreateWorkationVO workation = reservationMapper.selectWorkationForUpdate(
                userId,
                request.getWorkationId()
        );
        if (workation == null) {
            throw new BusinessException(ReservationErrorCode.WORKATION_NOT_AVAILABLE);
        }

        ReservationCreateProductVO product = reservationMapper.selectReservationProductForUpdate(
                request.getProductId()
        );
        if (product == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_PRODUCT_NOT_FOUND);
        }

        validateProductConfiguration(product);
        validateReservationPeriod(request, workation, product.getProductDetailType());
        validateReservationRegion(workation, product);
        validateHeadcount(request, product);

//        공유 오피스면 예약 마지막 날짜 포함
        boolean includeEndDate = product.getProductDetailType() != ReservationProductDetailType.ROOM;
        int overlappingCount = reservationMapper.countOverlappingReservation(
                userId,
                request.getWorkationId(),
                product.getMerchantCategory(),
                request.getStartDate(),
                request.getEndDate(),
                includeEndDate
        );
        if (overlappingCount > 0) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_CATEGORY_OVERLAP);
        }

        long usageDays = calculateUsageDays(
                request.getStartDate(),
                request.getEndDate(),
                includeEndDate
        );
        int reservedCount = calculateReservedCount(request, product.getProductDetailType());
        List<ReservationDailyInventoryVO> inventories =
                reservationMapper.selectDailyInventoriesForUpdate(
                        request.getProductId(),
                        request.getStartDate(),
                        request.getEndDate(),
                        includeEndDate
                );
        validateInventories(inventories, usageDays, reservedCount);

        BigDecimal totalAmount = calculateTotalAmount(request, product, usageDays);
        validateTotalAmount(totalAmount);

        ReservationCreateVO reservation = createReservation(userId, request, totalAmount);
        int insertedRows = reservationMapper.insertReservation(reservation);
        if (insertedRows != 1 || reservation.getId() == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_PROCESSING_FAILED);
        }

        String reservationCode = createReservationCode(
                reservation.getId(),
                LocalDate.now(SEOUL_ZONE_ID)
        );
        reservation.setReservationCode(reservationCode);
        int codeUpdatedRows = reservationMapper.updateReservationCode(
                reservation.getId(),
                reservationCode
        );
        if (codeUpdatedRows != 1) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_PROCESSING_FAILED);
        }

        PaymentRequest paymentRequest = createPaymentRequest(
                request,
                reservation,
                product,
                totalAmount
        );
        paymentService.pay(userId, paymentRequest);

        for (ReservationDailyInventoryVO inventory : inventories) {
            int updatedRows = reservationMapper.updateDailyInventory(
                    inventory.getId(),
                    reservedCount
            );
            if (updatedRows != 1) {
                throw new BusinessException(ReservationErrorCode.RESERVATION_INVENTORY_UNAVAILABLE);
            }

            int linkedRows = reservationMapper.insertReservationDailyInventory(
                    reservation.getId(),
                    inventory.getId(),
                    reservedCount
            );
            if (linkedRows != 1) {
                throw new BusinessException(ReservationErrorCode.RESERVATION_PROCESSING_FAILED);
            }
        }

        return ReservationCreateResponseDTO.from(reservation, product);
    }

//    예약 취소, 환불, 재고 복구를 하나의 트랜잭션으로 묶음
    @Override
    @Transactional
    public ReservationCancelResponseDTO saveReservationCancellation(
            Long userId,
            Long reservationId) {

        validateDetailRequest(userId, reservationId);

        ReservationCancelTargetVO target = reservationMapper
                .selectReservationCancelTargetForUpdate(userId, reservationId);
        if (target == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        validateCancellationTarget(target);

        CancelResponse cancelResponse = paymentService.cancelPayment(
                userId,
                target.getPaymentTransactionId()
        );
        validateCancellationResult(cancelResponse);

        List<ReservationCancelInventoryVO> inventories = reservationMapper
                .selectReservationCancelInventoriesForUpdate(reservationId);
        if (inventories == null || inventories.isEmpty()) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_INVENTORY_RESTORE_FAILED
            );
        }

        for (ReservationCancelInventoryVO inventory : inventories) {
            int restoredRows = reservationMapper.updateDailyInventoryForCancellation(
                    inventory.getDailyInventoryId(),
                    inventory.getReservedCount()
            );
            if (restoredRows != 1) {
                throw new BusinessException(
                        ReservationErrorCode.RESERVATION_INVENTORY_RESTORE_FAILED
                );
            }
        }

        BigDecimal cancelFee = BigDecimal.ZERO;
        ReservationCancelVO reservationCancel = new ReservationCancelVO();
        reservationCancel.setReservationId(reservationId);
        reservationCancel.setCancelFee(cancelFee);
        reservationCancel.setRefundAmount(target.getTotalAmount());
        reservationCancel.setCanceledAt(cancelResponse.getCancelledAt());
        reservationCancel.setRefundedAt(cancelResponse.getCancelledAt());

        int insertedRows = reservationMapper.insertReservationCancel(reservationCancel);
        if (insertedRows != 1) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CANCEL_PROCESSING_FAILED
            );
        }

        int updatedRows = reservationMapper.updateReservationStatusToCanceled(reservationId);
        if (updatedRows != 1) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CANCEL_PROCESSING_FAILED
            );
        }

        return ReservationCancelResponseDTO.from(
                target,
                cancelFee,
                target.getTotalAmount(),
                cancelResponse.getCancelledAt()
        );
    }

//    사용자 예약 목록 조회
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ReservationListItemResponseDTO> findReservationList(
            Long userId,
            Long workationId,
            List<ReservationStatus> statuses,
            ReservationCategory category,
            int page,
            int size) {

        validateRequest(userId, workationId, statuses, page, size);

        // 같은 상태가 여러 번 전달돼도 SQL IN 조건에는 한 번만 포함
        List<ReservationStatus> distinctStatuses = new ArrayList<>(new LinkedHashSet<>(statuses));
        int offset = page * size;

        // 목록과 동일한 사용자·상태·카테고리 조건으로 전체 건수를 조회
        long totalElements = reservationMapper.countReservationList(
                userId,
                workationId,
                distinctStatuses,
                category
        );

        if (totalElements == 0) {
            return PageResponseDTO.of(Collections.emptyList(), page, size, 0);
        }

        // CANCELED만 조회할 때는 취소 일시 기준, 그 외에는 이용 시작 일시 기준으로 정렬
        boolean canceledOnly = distinctStatuses.size() == 1
                && distinctStatuses.contains(ReservationStatus.CANCELED);

        List<ReservationListItemResponseDTO> content = reservationMapper
                .selectReservationList(
                        userId,
                        workationId,
                        distinctStatuses,
                        category,
                        offset,
                        size,
                        canceledOnly
                )
                .stream()
                .map(ReservationListItemResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.of(content, page, size, totalElements);
    }

    // 예약 상품의 날짜별 남은 재고와 예약 가능 여부 조회
    @Override
    @Transactional(readOnly = true)
    public List<ReservationProductAvailabilityResponseDTO>
    findReservationProductAvailabilities(
            Long productId,
            ReservationProductAvailabilityRequestDTO request) {

        validateAvailabilityRequest(productId, request);

        ReservationProductDetailType productDetailType =
                reservationMapper.selectReservationProductDetailType(productId);
        if (productDetailType == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_PRODUCT_NOT_FOUND);
        }

        boolean includeEndDate = productDetailType != ReservationProductDetailType.ROOM;
        validateAvailabilityPeriod(request, productDetailType);

        List<ReservationDailyInventoryVO> inventories =
                reservationMapper.selectDailyInventories(
                        productId,
                        request.getStartDate(),
                        request.getEndDate(),
                        includeEndDate
                );

        Map<LocalDate, ReservationDailyInventoryVO> inventoryByDate = new HashMap<>();
        for (ReservationDailyInventoryVO inventory : inventories) {
            inventoryByDate.put(inventory.getInventoryDate(), inventory);
        }

        return createAvailabilityResponses(
                request.getStartDate(),
                request.getEndDate(),
                includeEndDate,
                inventoryByDate
        );
    }

//    사용자 예약 상세 정보 조회
    @Override
    @Transactional(readOnly = true)
    public ReservationDetailResponseDTO findReservationDetails(Long userId, Long reservationId) {
        validateDetailRequest(userId, reservationId);

        ReservationDetailVO detail = reservationMapper.selectReservationDetails(userId, reservationId);
        if (detail == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }

        LocalDate today = LocalDate.now(SEOUL_ZONE_ID);
        LocalDateTime reviewDeadline = detail.getEndDate()
                .plusDays(30)
                .atTime(LocalTime.MAX);

//        예약이 확정된(CONFIRMED) 상태이고 이용 시작일 이전이면 취소 가능
        boolean cancelable = detail.getStatus() == ReservationStatus.CONFIRMED
                && today.isBefore(detail.getStartDate());

        // 숙소는 체크아웃 당일, 공유 오피스는 이용 종료 다음 날부터 작성 가능
        boolean reviewStartReached = "ACCOMMODATION".equals(detail.getMerchantCategory())
                ? !today.isBefore(detail.getEndDate())
                : today.isAfter(detail.getEndDate());
        boolean reviewDeadlineNotPassed = !today.isAfter(detail.getEndDate().plusDays(30));
        boolean reviewPeriod = reviewStartReached && reviewDeadlineNotPassed;

//        후기 작성 가능 여부와 기존 후기 존재 여부를 바탕으로 화면 동작을 결정
        boolean activeReview = detail.getReviewId() != null
                && "ACTIVE".equals(detail.getReviewStatus());

        ReservationReviewAction reviewAction = findReviewAction(
                detail,
                reviewPeriod,
                reviewDeadlineNotPassed,
                activeReview
        );
        Long activeReviewId = activeReview ? detail.getReviewId() : null;

        return ReservationDetailResponseDTO.from(
                detail,
                cancelable,
                activeReviewId,
                reviewAction,
                reviewDeadline
        );
    }

//    userId 사용자의 reservationId 기준 예약 취소 상세 조회
    @Override
    @Transactional(readOnly = true)
    public ReservationCancellationDetailResponseDTO findReservationCancellationDetails(
            Long userId,
            Long reservationId) {

        validateDetailRequest(userId, reservationId);

        ReservationCancellationDetailVO detail =
                reservationMapper.selectReservationCancellationDetails(userId, reservationId);
        if (detail == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_CANCELLATION_NOT_FOUND);
        }

        return ReservationCancellationDetailResponseDTO.from(detail);
    }

    // 예약 생성 필수값과 양수 조건을 검증
    private void validateCreateRequest(Long userId, ReservationCreateRequestDTO request) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (request == null
                || request.getProductId() == null
                || request.getProductId() < 1
                || request.getWorkationId() == null
                || request.getWorkationId() < 1
                || request.getStartDate() == null
                || request.getEndDate() == null
                || request.getHeadcount() == null
                || request.getHeadcount() < 1
                || request.getQuantity() == null
                || request.getQuantity() < 1) {

            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_REQUEST);
        }
    }

    // 예약 상태와 이용 시작일 및 결제 거래의 취소 가능 조건 검증
    private void validateCancellationTarget(ReservationCancelTargetVO target) {
        if (target.getStatus() == ReservationStatus.CANCELED) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_ALREADY_CANCELED);
        }
        if (target.getStatus() != ReservationStatus.CONFIRMED) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_CANCEL_NOT_ALLOWED);
        }
        if (!LocalDate.now(SEOUL_ZONE_ID).isBefore(target.getStartDate())) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_CANCEL_PERIOD_EXPIRED
            );
        }
        if (target.getPaymentTransactionId() == null) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_PAYMENT_NOT_FOUND);
        }
    }

    // 거래 취소 결과의 완료 상태와 취소 일시 검증
    private void validateCancellationResult(CancelResponse cancelResponse) {

        if (cancelResponse == null
                || !"CANCELED".equals(cancelResponse.getStatus())
                || cancelResponse.getCancelledAt() == null) {

            throw new BusinessException(ReservationErrorCode.RESERVATION_REFUND_FAILED);
        }
    }

    // 상품 유형과 가맹점 카테고리 및 단가의 조합 검증
    private void validateProductConfiguration(ReservationCreateProductVO product) {
        boolean validRoom = product.getProductDetailType() == ReservationProductDetailType.ROOM
                && product.getMerchantCategory() == ReservationCategory.ACCOMMODATION;
        boolean validMeetingRoom = product.getProductDetailType() == ReservationProductDetailType.MEETING_ROOM
                && product.getMerchantCategory() == ReservationCategory.OFFICE;
        boolean validOfficeSeat = product.getProductDetailType() == ReservationProductDetailType.OFFICE_SEAT
                && product.getMerchantCategory() == ReservationCategory.OFFICE;
        boolean validCommonValues = product.getMaxHeadcount() != null
                && product.getMaxHeadcount() > 0
                && product.getUnitPrice() != null
                && product.getUnitPrice().compareTo(BigDecimal.ZERO) > 0;

        if (!validCommonValues || (!validRoom && !validMeetingRoom && !validOfficeSeat)) {
            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_PRODUCT_CONFIGURATION_INVALID
            );
        }
    }

    // 숙소는 종료일을 제외, 공유오피스는 종료일을 포함하는 기간 규칙을 검증
    private void validateReservationPeriod(
            ReservationCreateRequestDTO request,
            ReservationCreateWorkationVO workation,
            ReservationProductDetailType productDetailType) {

        boolean invalidDateOrder = productDetailType == ReservationProductDetailType.ROOM
                ? !request.getStartDate().isBefore(request.getEndDate())
                : request.getStartDate().isAfter(request.getEndDate());
        if (invalidDateOrder) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_DATE);
        }

        if (request.getStartDate().isBefore(workation.getStartDate())
                || request.getEndDate().isAfter(workation.getEndDate())) {

            throw new BusinessException(ReservationErrorCode.RESERVATION_DATE_OUT_OF_WORKATION);
        }
    }

    // 워케이션 등록 지역과 예약 상품의 가맹점 지역이 같은지 검증
    private void validateReservationRegion(
            ReservationCreateWorkationVO workation,
            ReservationCreateProductVO product) {

        if (!workation.getRegionId().equals(product.getMerchantRegionId())) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_REGION_MISMATCH);
        }
    }

    // 객실과 회의실은 수량별 수용 인원으로 검증한다.
    //
    // 좌석은 1인 1석이라 상품의 최대 인원으로 막으면 2명 이상이 예약할 수 없다.
    // 재고를 인원만큼 차감하고 금액도 인원만큼 곱하므로(calculateReservedCount, calculateTotalAmount)
    // 인원 상한은 validateInventories 의 남은 좌석 검증이 담당한다
    private void validateHeadcount(
            ReservationCreateRequestDTO request,
            ReservationCreateProductVO product) {

        if (product.getProductDetailType() == ReservationProductDetailType.OFFICE_SEAT) {
            return;
        }

        long maximumHeadcount = (long) product.getMaxHeadcount() * request.getQuantity();
        if (request.getHeadcount() > maximumHeadcount) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_HEADCOUNT_EXCEEDED);
        }
    }

    // 종료일 포함 여부에 따라 결제와 재고 차감 대상 일수를 계산
    private long calculateUsageDays(
            LocalDate startDate,
            LocalDate endDate,
            boolean includeEndDate) {

        long usageDays = ChronoUnit.DAYS.between(startDate, endDate);
        return includeEndDate ? usageDays + 1 : usageDays;
    }

    // 객실과 회의실은 수량, 좌석은 이용 인원만큼 날짜별 재고를 차감
    private int calculateReservedCount(
            ReservationCreateRequestDTO request,
            ReservationProductDetailType productDetailType) {

        return productDetailType == ReservationProductDetailType.OFFICE_SEAT
                ? request.getHeadcount()
                : request.getQuantity();
    }

    // 대상 날짜가 모두 존재하고 각 날짜에 충분한 재고가 있는지 검증
    private void validateInventories(
            List<ReservationDailyInventoryVO> inventories,
            long usageDays,
            int reservedCount) {

        if (usageDays > Integer.MAX_VALUE || inventories.size() != (int) usageDays) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_INVENTORY_UNAVAILABLE);
        }

        for (ReservationDailyInventoryVO inventory : inventories) {
            if (!Boolean.TRUE.equals(inventory.getAvailable())
                    || inventory.getRemainingCapacity() == null
                    || inventory.getRemainingCapacity() < reservedCount) {

                throw new BusinessException(ReservationErrorCode.RESERVATION_INVENTORY_UNAVAILABLE);
            }
        }
    }

    // 상품 유형별 합의된 계산식으로 서버의 최종 결제 금액을 계산
    private BigDecimal calculateTotalAmount(
            ReservationCreateRequestDTO request,
            ReservationCreateProductVO product,
            long usageDays) {

        int multiplier = product.getProductDetailType() == ReservationProductDetailType.OFFICE_SEAT
                ? request.getHeadcount()
                : request.getQuantity();
        return product.getUnitPrice()
                .multiply(BigDecimal.valueOf(usageDays))
                .multiply(BigDecimal.valueOf(multiplier));
    }

    // API의 금액 응답과 결제 조건을 만족하는 금액인지 검증
    private void validateTotalAmount(BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0
                || totalAmount.stripTrailingZeros().scale() > 0) {

            throw new BusinessException(
                    ReservationErrorCode.RESERVATION_PRODUCT_CONFIGURATION_INVALID
            );
        }
    }

    // 결제 실패 시 함께 롤백될 확정 예약 객체를 생성
    private ReservationCreateVO createReservation(
            Long userId,
            ReservationCreateRequestDTO request,
            BigDecimal totalAmount) {

        ReservationCreateVO reservation = new ReservationCreateVO();
        reservation.setUserId(userId);
        reservation.setWorkationId(request.getWorkationId());
        reservation.setProductId(request.getProductId());
        reservation.setReservationCode("TMP-" + UUID.randomUUID());
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setHeadcount(request.getHeadcount());
        reservation.setQuantity(request.getQuantity());
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        return reservation;
    }

    // 클라이언트가 선택한 결제 수단과 서버가 확정한 예약 결제 정보를 조합
    private PaymentRequest createPaymentRequest(
            ReservationCreateRequestDTO request,
            ReservationCreateVO reservation,
            ReservationCreateProductVO product,
            BigDecimal totalAmount) {

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setReservationId(reservation.getId());
        paymentRequest.setMerchantId(product.getMerchantId());
        paymentRequest.setMerchantName(product.getMerchantName());
        paymentRequest.setAmount(totalAmount);
        paymentRequest.setPaymentSourceType(
                request.getPaymentSourceType() == null
                        ? null
                        : request.getPaymentSourceType().name()
        );
        paymentRequest.setCardId(request.getCardId());
        paymentRequest.setPinNumber(request.getPinNumber());
        paymentRequest.setDeviceId(findPaymentDeviceId(request.getDeviceId()));
        paymentRequest.setIdempotencyKey(request.getIdempotencyKey());
        return paymentRequest;
    }

    // 요청값을 우선하고 비어 있을 때만 로컬 테스트용 기기 식별자를 사용하는 선택값
    private String findPaymentDeviceId(String deviceId) {
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            return deviceId;
        }
        return devDefaultDeviceId;
    }

    // 생성일과 전역 예약 PK를 조합해 WR-yyyyMMdd-000001 형식의 예약번호 생성
    private String createReservationCode(Long reservationId, LocalDate creationDate) {
        if (reservationId > MAX_RESERVATION_SEQUENCE) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_CODE_EXHAUSTED);
        }

        return String.format(
                "WR-%s-%06d",
                creationDate.format(RESERVATION_DATE_FORMAT),
                reservationId
        );
    }

    // 리뷰 작성 이력과 이용 종료 후 30일 기한을 기준으로 화면 동작을 결정
    private ReservationReviewAction findReviewAction(
            ReservationDetailVO detail,
            boolean reviewPeriod,
            boolean reviewDeadlineNotPassed,
            boolean activeReview) {

        if (activeReview) {
            return reviewDeadlineNotPassed
                    ? ReservationReviewAction.EDIT
                    : ReservationReviewAction.DELETE;
        }

        if (reviewPeriod && detail.getReviewId() == null) {
            return ReservationReviewAction.WRITE;
        }

        return ReservationReviewAction.NONE;
    }

    // 상세 조회에 필요한 사용자와 예약 식별자를 검증
    private void validateDetailRequest(Long userId, Long reservationId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (reservationId == null || reservationId < 1) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_ID);
        }
    }

    // 필수 조회 조건과 페이징 범위를 검증
    private void validateRequest(
            Long userId,
            Long workationId,
            List<ReservationStatus> statuses,
            int page,
            int size) {

        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("사용자 정보가 올바르지 않습니다.");
        }

        if (workationId != null && workationId < 1) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_REQUEST);
        }

        if (statuses == null || statuses.isEmpty() || statuses.contains(null)) {
            throw new BusinessException(ReservationErrorCode.RESERVATION_STATUS_REQUIRED);
        }

        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 50 이하여야 합니다.");
        }

        if (page > Integer.MAX_VALUE / size) {
            throw new IllegalArgumentException("요청한 페이지 범위가 너무 큽니다.");
        }
    }

    // 예약 상품과 날짜별 재고 조회 필수값 검증
    private void validateAvailabilityRequest(
            Long productId,
            ReservationProductAvailabilityRequestDTO request) {

        if (productId == null || productId < 1) {
            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_REQUEST);
        }

        if (request == null
                || request.getStartDate() == null
                || request.getEndDate() == null
                || request.getStartDate().isAfter(request.getEndDate())) {

            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_DATE);
        }
    }

    // 숙소는 체크아웃 날짜를 제외하고 공유오피스는 종료일을 포함하는 기간 검증
    private void validateAvailabilityPeriod(
            ReservationProductAvailabilityRequestDTO request,
            ReservationProductDetailType productDetailType) {

        if (productDetailType == ReservationProductDetailType.ROOM
                && !request.getStartDate().isBefore(request.getEndDate())) {

            throw new BusinessException(ReservationErrorCode.INVALID_RESERVATION_DATE);
        }
    }

    // 재고가 등록되지 않은 대상 날짜를 예약 불가 상태로 보완한 응답 목록
    private List<ReservationProductAvailabilityResponseDTO> createAvailabilityResponses(
            LocalDate startDate,
            LocalDate endDate,
            boolean includeEndDate,
            Map<LocalDate, ReservationDailyInventoryVO> inventoryByDate) {

        List<ReservationProductAvailabilityResponseDTO> responses = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (currentDate.isBefore(endDate)) {
            responses.add(ReservationProductAvailabilityResponseDTO.from(
                    currentDate,
                    inventoryByDate.get(currentDate)
            ));
            currentDate = currentDate.plusDays(1);
        }

        if (includeEndDate) {
            responses.add(ReservationProductAvailabilityResponseDTO.from(
                    endDate,
                    inventoryByDate.get(endDate)
            ));
        }

        return responses;
    }
}
