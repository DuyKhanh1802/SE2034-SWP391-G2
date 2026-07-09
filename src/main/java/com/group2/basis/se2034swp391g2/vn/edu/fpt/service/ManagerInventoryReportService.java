package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.InventoryTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.InventoryReportRowResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.InventoryReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryItemRepository;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.InventoryReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerInventoryReportService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReceiptRepository inventoryReceiptRepository;

    @Transactional(readOnly = true)
    public List<InventoryReportRowResponse> getInventoryReportRows(LocalDate fromDate,
                                                                   LocalDate toDate,
                                                                   Long categoryId,
                                                                   String keyword,
                                                                   String stockStatus,
                                                                   String sortBy) {
        validateDateRange(fromDate, toDate);

        Instant fromInstant = fromDate.atStartOfDay(APP_ZONE).toInstant();
        Instant toInstant = toDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedStockStatus = normalizeStockStatus(stockStatus);

        List<InventoryReportRowResponse> rows = inventoryItemRepository.findInventoryReportRows(
                fromInstant,
                toInstant,
                fromDate,
                toDate,
                categoryId,
                normalizedKeyword,
                normalizedStockStatus,
                InventoryTransactionType.IN,
                InventoryTransactionType.OUT,
                InventoryTransactionType.DISPOSAL
        );

        attachLatestExpiryDate(rows);
        sortRows(rows, sortBy);

        return rows;
    }

    public InventoryReportSummaryResponse buildSummary(List<InventoryReportRowResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return new InventoryReportSummaryResponse(
                    0,
                    BigDecimal.ZERO,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    0,
                    0
            );
        }

        long totalItemCount = rows.size();

        BigDecimal totalStockValue = rows.stream()
                .map(InventoryReportRowResponse::getStockValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long lowStockCount = rows.stream()
                .filter(row -> "LOW".equals(row.getStockStatus()))
                .count();

        long outOfStockCount = rows.stream()
                .filter(row -> "OUT_OF_STOCK".equals(row.getStockStatus()))
                .count();

        BigDecimal totalReceivedQuantity = rows.stream()
                .map(InventoryReportRowResponse::getReceivedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOutQuantity = rows.stream()
                .map(InventoryReportRowResponse::getTotalOutQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long expiringSoonCount = rows.stream()
                .filter(row -> "EXPIRING_SOON".equals(row.getExpiryStatus()))
                .count();

        long expiredCount = rows.stream()
                .filter(row -> "EXPIRED".equals(row.getExpiryStatus()))
                .count();

        return new InventoryReportSummaryResponse(
                totalItemCount,
                totalStockValue,
                lowStockCount,
                outOfStockCount,
                totalReceivedQuantity,
                totalOutQuantity,
                expiringSoonCount,
                expiredCount
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

    private String normalizeStockStatus(String stockStatus) {
        if (stockStatus == null || stockStatus.isBlank() || "ALL".equalsIgnoreCase(stockStatus)) {
            return null;
        }

        String normalized = stockStatus.trim().toUpperCase();

        if (!"OUT_OF_STOCK".equals(normalized)
                && !"LOW".equals(normalized)
                && !"NORMAL".equals(normalized)) {
            return null;
        }

        return normalized;
    }

    private void attachLatestExpiryDate(List<InventoryReportRowResponse> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        List<Long> itemIds = rows.stream()
                .map(InventoryReportRowResponse::getItemId)
                .filter(id -> id != null)
                .toList();

        if (itemIds.isEmpty()) {
            return;
        }

        Map<Long, LocalDate> expiryDateByItemId = inventoryReceiptRepository
                .findLatestExpiryDatesByItemIds(itemIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> toLocalDate(row[1]),
                        (first, ignored) -> first
                ));

        rows.forEach(row -> row.setLatestExpiryDate(expiryDateByItemId.get(row.getItemId())));
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof LocalDate localDate) {
            return localDate;
        }

        if (value instanceof Date date) {
            return date.toLocalDate();
        }

        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(APP_ZONE).toLocalDate();
        }

        throw new IllegalArgumentException("Unsupported expiry date value: " + value);
    }

    private void sortRows(List<InventoryReportRowResponse> rows, String sortBy) {
        if (rows == null || rows.isEmpty()) {
            return;
        }

        if ("stockAsc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparing(InventoryReportRowResponse::getCurrentQuantity));
            return;
        }

        if ("valueDesc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparing(InventoryReportRowResponse::getStockValue).reversed());
            return;
        }

        if ("receivedDesc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparing(InventoryReportRowResponse::getReceivedQuantity).reversed());
            return;
        }

        if ("nameAsc".equalsIgnoreCase(sortBy)) {
            rows.sort(Comparator.comparing(InventoryReportRowResponse::getItemName, String.CASE_INSENSITIVE_ORDER));
            return;
        }

        rows.sort(Comparator.comparing(InventoryReportRowResponse::getStockStatus)
                .thenComparing(InventoryReportRowResponse::getCurrentQuantity));
    }
}