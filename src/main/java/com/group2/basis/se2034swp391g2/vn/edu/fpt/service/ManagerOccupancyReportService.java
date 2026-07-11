package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.BookingDetail;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.Room;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.RoomTypeVariant;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.OccupancyReportResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.OccupancyReportRowResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.OccupancyReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.BookingDetailRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.RoomTypeVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ManagerOccupancyReportService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int OCCUPANCY_REPORT_PAGE_SIZE = 10;
    private static final List<BookingStatus> OCCUPANCY_STATUSES = List.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.CHECKED_OUT
    );
    private static final String INVALID_DATE_RANGE_MESSAGE = "Ngày bắt đầu không được sau ngày kết thúc.";

    private final RoomRepository roomRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RoomTypeVariantRepository roomTypeVariantRepository;

    @Transactional(readOnly = true)
    public OccupancyReportResponse getOccupancyReport(LocalDate fromDate,
                                                      LocalDate toDate,
                                                      Long variantId,
                                                      String sortBy,
                                                      int page) {
        LocalDate resolvedFromDate = resolveFromDate(fromDate);
        LocalDate resolvedToDate = resolveToDate(toDate);
        Long selectedVariantId = normalizeVariantId(variantId);
        String errorMessage = null;

        if (resolvedFromDate.isAfter(resolvedToDate)) {
            errorMessage = INVALID_DATE_RANGE_MESSAGE;
            resolvedFromDate = getDefaultFromDate();
            resolvedToDate = getDefaultToDate();
        }

        LocalDate toExclusiveDate = resolvedToDate.plusDays(1);
        long reportDays = ChronoUnit.DAYS.between(resolvedFromDate, toExclusiveDate);

        Map<Long, RowAccumulator> rowsByVariant = buildRoomCapacityRows(reportDays, selectedVariantId);
        List<BookingDetail> bookingDetails = bookingDetailRepository.findOccupancyReportDetails(
                resolvedFromDate,
                toExclusiveDate,
                OCCUPANCY_STATUSES
        );

        Set<Long> reportBookingIds = new HashSet<>();

        for (BookingDetail detail : bookingDetails) {
            RoomTypeVariant variant = detail.getVariant();
            if (variant == null || variant.getId() == null) {
                continue;
            }

            if (selectedVariantId != null && !selectedVariantId.equals(variant.getId())) {
                continue;
            }

            RowAccumulator row = rowsByVariant.computeIfAbsent(
                    variant.getId(),
                    rowVariantId -> new RowAccumulator(variant)
            );

            long occupiedNights = calculateOverlapNights(
                    detail.getCheckInDate(),
                    detail.getCheckOutDate(),
                    resolvedFromDate,
                    toExclusiveDate
            );

            if (occupiedNights <= 0) {
                continue;
            }

            row.occupiedRoomNights += occupiedNights;
            if (detail.getBooking() != null && detail.getBooking().getId() != null) {
                row.bookingIds.add(detail.getBooking().getId());
                reportBookingIds.add(detail.getBooking().getId());
            }
        }

        List<OccupancyReportRowResponse> allRows = rowsByVariant.values()
                .stream()
                .map(RowAccumulator::toResponse)
                .sorted(buildRowComparator(sortBy))
                .toList();

        OccupancyReportSummaryResponse summary = buildSummary(allRows, reportBookingIds.size());
        int totalItems = allRows.size();
        int totalPages = totalItems == 0
                ? 1
                : (int) Math.ceil((double) totalItems / OCCUPANCY_REPORT_PAGE_SIZE);
        int currentPage = Math.max(0, Math.min(page, totalPages - 1));
        int startIndex = currentPage * OCCUPANCY_REPORT_PAGE_SIZE;
        int endIndex = Math.min(startIndex + OCCUPANCY_REPORT_PAGE_SIZE, totalItems);
        List<OccupancyReportRowResponse> pageRows = totalItems == 0
                ? List.of()
                : allRows.subList(startIndex, endIndex);

        return new OccupancyReportResponse(
                resolvedFromDate,
                resolvedToDate,
                reportDays,
                summary,
                pageRows,
                currentPage,
                OCCUPANCY_REPORT_PAGE_SIZE,
                totalItems,
                totalPages,
                totalItems == 0 ? 0 : startIndex + 1,
                endIndex,
                errorMessage
        );
    }

    public OccupancyReportResponse getOccupancyReport(LocalDate fromDate, LocalDate toDate, int page) {
        return getOccupancyReport(fromDate, toDate, null, null, page);
    }

    public OccupancyReportResponse getOccupancyReport(LocalDate fromDate, LocalDate toDate) {
        return getOccupancyReport(fromDate, toDate, null, null, 0);
    }

    public List<RoomTypeVariant> getRoomTypeVariantsForFilter() {
        return roomTypeVariantRepository.findByIsDeletedFalseOrderByRoomType_NameAscVariantNameAsc();
    }

    public LocalDate resolveFromDate(LocalDate fromDate) {
        if (fromDate != null) {
            return fromDate;
        }

        return getDefaultFromDate();
    }

    public LocalDate resolveToDate(LocalDate toDate) {
        if (toDate != null) {
            return toDate;
        }

        return getDefaultToDate();
    }

    private Map<Long, RowAccumulator> buildRoomCapacityRows(long reportDays, Long selectedVariantId) {
        Map<Long, RowAccumulator> rowsByVariant = new HashMap<>();

        for (Room room : roomRepository.findByIsDeletedFalse()) {
            if (room.getStatus() == RoomStatus.MAINTENANCE || room.getVariant() == null || room.getVariant().getId() == null) {
                continue;
            }

            if (selectedVariantId != null && !selectedVariantId.equals(room.getVariant().getId())) {
                continue;
            }

            RowAccumulator row = rowsByVariant.computeIfAbsent(
                    room.getVariant().getId(),
                    variantId -> new RowAccumulator(room.getVariant())
            );

            row.totalRooms++;
            row.availableRoomNights += reportDays;
        }

        return rowsByVariant;
    }

    private OccupancyReportSummaryResponse buildSummary(List<OccupancyReportRowResponse> rows, long bookingCount) {
        long totalRooms = rows.stream()
                .mapToLong(OccupancyReportRowResponse::getTotalRooms)
                .sum();

        long availableRoomNights = rows.stream()
                .mapToLong(OccupancyReportRowResponse::getAvailableRoomNights)
                .sum();

        long occupiedRoomNights = rows.stream()
                .mapToLong(OccupancyReportRowResponse::getOccupiedRoomNights)
                .sum();

        String bestVariant = rows.stream()
                .filter(row -> row.getAvailableRoomNights() > 0)
                .max(Comparator.comparing(OccupancyReportRowResponse::getOccupancyRate))
                .map(OccupancyReportRowResponse::getVariantName)
                .orElse("Không có dữ liệu");

        return new OccupancyReportSummaryResponse(
                totalRooms,
                availableRoomNights,
                occupiedRoomNights,
                bookingCount,
                bestVariant
        );
    }

    private Comparator<OccupancyReportRowResponse> buildRowComparator(String sortBy) {
        Comparator<OccupancyReportRowResponse> defaultComparator = Comparator
                .comparing(OccupancyReportRowResponse::getRoomTypeName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(OccupancyReportRowResponse::getVariantName, String.CASE_INSENSITIVE_ORDER);

        if (sortBy == null || sortBy.isBlank()) {
            return defaultComparator;
        }

        return switch (sortBy) {
            case "occupancyDesc" -> Comparator
                    .comparing(OccupancyReportRowResponse::getOccupancyRate)
                    .reversed()
                    .thenComparing(defaultComparator);
            case "occupancyAsc" -> Comparator
                    .comparing(OccupancyReportRowResponse::getOccupancyRate)
                    .thenComparing(defaultComparator);
            case "occupiedNightsDesc" -> Comparator
                    .comparingLong(OccupancyReportRowResponse::getOccupiedRoomNights)
                    .reversed()
                    .thenComparing(defaultComparator);
            case "occupiedNightsAsc" -> Comparator
                    .comparingLong(OccupancyReportRowResponse::getOccupiedRoomNights)
                    .thenComparing(defaultComparator);
            default -> defaultComparator;
        };
    }

    private long calculateOverlapNights(LocalDate bookingFromDate,
                                        LocalDate bookingToDate,
                                        LocalDate reportFromDate,
                                        LocalDate reportToExclusiveDate) {
        if (bookingFromDate == null || bookingToDate == null) {
            return 0;
        }

        LocalDate overlapFromDate = bookingFromDate.isAfter(reportFromDate) ? bookingFromDate : reportFromDate;
        LocalDate overlapToDate = bookingToDate.isBefore(reportToExclusiveDate) ? bookingToDate : reportToExclusiveDate;

        if (!overlapToDate.isAfter(overlapFromDate)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(overlapFromDate, overlapToDate);
    }

    private Long normalizeVariantId(Long variantId) {
        if (variantId == null || variantId <= 0) {
            return null;
        }

        return variantId;
    }

    private LocalDate getDefaultFromDate() {
        return bookingDetailRepository.findEarliestOccupancyCheckInDate(OCCUPANCY_STATUSES)
                .orElse(LocalDate.now(APP_ZONE));
    }

    private LocalDate getDefaultToDate() {
        return bookingDetailRepository.findLatestOccupancyCheckOutDate(OCCUPANCY_STATUSES)
                .map(date -> date.minusDays(1))
                .orElse(LocalDate.now(APP_ZONE));
    }

    private static class RowAccumulator {
        private final Long variantId;
        private final String roomTypeName;
        private final String variantName;
        private long totalRooms;
        private long availableRoomNights;
        private long occupiedRoomNights;
        private final Set<Long> bookingIds = new HashSet<>();

        private RowAccumulator(RoomTypeVariant variant) {
            this.variantId = variant.getId();
            this.roomTypeName = getRoomTypeName(variant);
            this.variantName = variant.getVariantName() == null ? "Chưa đặt tên hạng phòng" : variant.getVariantName();
        }

        private OccupancyReportRowResponse toResponse() {
            return new OccupancyReportRowResponse(
                    variantId,
                    roomTypeName,
                    variantName,
                    totalRooms,
                    availableRoomNights,
                    occupiedRoomNights,
                    bookingIds.size()
            );
        }

        private static String getRoomTypeName(RoomTypeVariant variant) {
            RoomType roomType = variant.getRoomType();
            if (roomType == null || roomType.getName() == null) {
                return "Chưa phân loại";
            }

            return roomType.getName();
        }
    }
}
