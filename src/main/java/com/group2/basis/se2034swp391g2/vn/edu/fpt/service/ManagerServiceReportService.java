package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportRowResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.ServiceReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.FolioItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerServiceReportService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final FolioItemRepository folioItemRepository;

    @Transactional(readOnly = true)
    public List<ServiceReportRowResponse> getServiceReportRows(LocalDate fromDate,
                                                               LocalDate toDate,
                                                               Long categoryId,
                                                               String keyword,
                                                               String sortBy) {
        validateDateRange(fromDate, toDate);

        Instant fromInstant = fromDate.atStartOfDay(APP_ZONE).toInstant();
        Instant toExclusiveInstant = toDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        String normalizedKeyword = normalizeKeyword(keyword);

        List<ServiceReportRowResponse> rows = folioItemRepository.findServiceReportRows(
                fromInstant,
                toExclusiveInstant,
                categoryId,
                normalizedKeyword,
                FolioItemStatus.CANCELLED
        );

        sortRows(rows, sortBy);
        return rows;
    }

    public ServiceReportSummaryResponse buildSummary(List<ServiceReportRowResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return new ServiceReportSummaryResponse(
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    "Không có dữ liệu",
                    "Không có dữ liệu"
            );
        }

        long totalSoldQuantity = rows.stream()
                .mapToLong(ServiceReportRowResponse::getSoldQuantity)
                .sum();

        BigDecimal totalBaseRevenue = rows.stream()
                .map(ServiceReportRowResponse::getBaseRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalServiceCharge = rows.stream()
                .map(ServiceReportRowResponse::getServiceChargeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalVat = rows.stream()
                .map(ServiceReportRowResponse::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = rows.stream()
                .map(ServiceReportRowResponse::getTotalRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String bestSellingService = rows.stream()
                .max(Comparator.comparingLong(ServiceReportRowResponse::getSoldQuantity))
                .map(ServiceReportRowResponse::getServiceName)
                .orElse("Không có dữ liệu");

        String highestRevenueService = rows.stream()
                .max(Comparator.comparing(ServiceReportRowResponse::getTotalRevenue))
                .map(ServiceReportRowResponse::getServiceName)
                .orElse("Không có dữ liệu");

        return new ServiceReportSummaryResponse(
                rows.size(),
                totalSoldQuantity,
                totalBaseRevenue,
                totalServiceCharge,
                totalVat,
                totalRevenue,
                bestSellingService,
                highestRevenueService
        );
    }

    public LocalDate resolveFromDate(LocalDate fromDate) {
        if (fromDate != null) {
            return fromDate;
        }

        return LocalDate.now(APP_ZONE).withDayOfMonth(1);
    }

    public LocalDate resolveToDate(LocalDate toDate) {
        if (toDate != null) {
            return toDate;
        }

        return LocalDate.now(APP_ZONE);
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Khoảng thời gian báo cáo không hợp lệ.");
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc.");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }

        return keyword.trim().toLowerCase();
    }

    private void sortRows(List<ServiceReportRowResponse> rows, String sortBy) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        if ("quantityDesc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparingLong(ServiceReportRowResponse::getSoldQuantity).reversed());
            return;
        }

        if ("nameAsc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparing(ServiceReportRowResponse::getServiceName, String.CASE_INSENSITIVE_ORDER));
            return;
        }

        rows.sort(Comparator.comparing(ServiceReportRowResponse::getTotalRevenue).reversed());
    }
}
