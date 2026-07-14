package com.group2.basis.se2034swp391g2.vn.edu.fpt.service;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.CashTransaction;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RevenueReportResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RevenueReportRowResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response.RevenueReportSummaryResponse;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.CashTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManagerRevenueReportService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String INVALID_DATE_RANGE_MESSAGE = "Ngày bắt đầu không được sau ngày kết thúc.";
    private static final List<CashTransactionCategory> REVENUE_REPORT_CATEGORIES = List.of(
            CashTransactionCategory.DEPOSIT,
            CashTransactionCategory.BOOKING_PAYMENT,
            CashTransactionCategory.MANUAL_INCOME,
            CashTransactionCategory.MANUAL_EXPENSE,
            CashTransactionCategory.INVENTORY_PURCHASE
    );

    private final CashTransactionRepository cashTransactionRepository;

    @Transactional(readOnly = true)
    public RevenueReportResponse getRevenueReport(LocalDate fromDate,
                                                  LocalDate toDate,
                                                  String sortBy) {
        LocalDate resolvedFromDate = resolveFromDate(fromDate);
        LocalDate resolvedToDate = resolveToDate(toDate);
        String errorMessage = null;

        if (resolvedFromDate.isAfter(resolvedToDate)) {
            errorMessage = INVALID_DATE_RANGE_MESSAGE;
            resolvedFromDate = getDefaultFromDate();
            resolvedToDate = getDefaultToDate();
        }

        Instant fromInstant = resolvedFromDate.atStartOfDay(APP_ZONE).toInstant();
        Instant toInstant = resolvedToDate.plusDays(1).atStartOfDay(APP_ZONE).toInstant();

        List<CashTransaction> transactions = cashTransactionRepository.findRevenueReportTransactions(
                CashTransactionStatus.COMPLETED,
                REVENUE_REPORT_CATEGORIES,
                fromInstant,
                toInstant
        );

        List<RevenueReportRowResponse> rows = buildRows(transactions)
                .stream()
                .sorted(buildComparator(sortBy))
                .toList();

        return new RevenueReportResponse(
                resolvedFromDate,
                resolvedToDate,
                buildSummary(rows),
                rows,
                errorMessage
        );
    }

    public LocalDate resolveFromDate(LocalDate fromDate) {
        return fromDate == null ? getDefaultFromDate() : fromDate;
    }

    public LocalDate resolveToDate(LocalDate toDate) {
        return toDate == null ? getDefaultToDate() : toDate;
    }

    private List<RevenueReportRowResponse> buildRows(List<CashTransaction> transactions) {
        Map<CashTransactionCategory, RowAccumulator> rowsByCategory = new EnumMap<>(CashTransactionCategory.class);

        for (CashTransaction transaction : transactions) {
            RowAccumulator row = rowsByCategory.computeIfAbsent(
                    transaction.getCategory(),
                    RowAccumulator::new
            );

            BigDecimal amount = transaction.getAmount() == null ? BigDecimal.ZERO : transaction.getAmount().abs();
            row.transactionCount++;

            if (transaction.getType() == CashTransactionType.EXPENSE) {
                row.expenseAmount = row.expenseAmount.add(amount);
            } else {
                row.incomeAmount = row.incomeAmount.add(amount);
            }
        }

        return rowsByCategory.values()
                .stream()
                .map(RowAccumulator::toResponse)
                .toList();
    }

    private RevenueReportSummaryResponse buildSummary(List<RevenueReportRowResponse> rows) {
        BigDecimal totalIncome = rows.stream()
                .map(RevenueReportRowResponse::getIncomeAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = rows.stream()
                .map(RevenueReportRowResponse::getExpenseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long transactionCount = rows.stream()
                .mapToLong(RevenueReportRowResponse::getTransactionCount)
                .sum();

        String highestIncomeCategory = rows.stream()
                .filter(row -> row.getIncomeAmount().compareTo(BigDecimal.ZERO) > 0)
                .max(Comparator.comparing(RevenueReportRowResponse::getIncomeAmount))
                .map(row -> row.getCategory().getDisplayName())
                .orElse("Không có dữ liệu");

        return new RevenueReportSummaryResponse(
                totalIncome,
                totalExpense,
                transactionCount,
                highestIncomeCategory
        );
    }

    private Comparator<RevenueReportRowResponse> buildComparator(String sortBy) {
        Comparator<RevenueReportRowResponse> defaultComparator = Comparator
                .comparing(row -> row.getCategory().getDisplayName(), String.CASE_INSENSITIVE_ORDER);

        if (sortBy == null || sortBy.isBlank()) {
            return defaultComparator;
        }

        return switch (sortBy) {
            case "incomeDesc" -> Comparator
                    .comparing(RevenueReportRowResponse::getIncomeAmount)
                    .reversed()
                    .thenComparing(defaultComparator);
            case "expenseDesc" -> Comparator
                    .comparing(RevenueReportRowResponse::getExpenseAmount)
                    .reversed()
                    .thenComparing(defaultComparator);
            case "netDesc" -> Comparator
                    .comparing(RevenueReportRowResponse::getNetAmount)
                    .reversed()
                    .thenComparing(defaultComparator);
            case "countDesc" -> Comparator
                    .comparingLong(RevenueReportRowResponse::getTransactionCount)
                    .reversed()
                    .thenComparing(defaultComparator);
            default -> defaultComparator;
        };
    }

    private LocalDate getDefaultFromDate() {
        return LocalDate.now(APP_ZONE).withDayOfMonth(1);
    }

    private LocalDate getDefaultToDate() {
        return LocalDate.now(APP_ZONE);
    }

    private static class RowAccumulator {
        private final CashTransactionCategory category;
        private final CashTransactionType type;
        private long transactionCount;
        private BigDecimal incomeAmount = BigDecimal.ZERO;
        private BigDecimal expenseAmount = BigDecimal.ZERO;

        private RowAccumulator(CashTransactionCategory category) {
            this.category = category;
            this.type = switch (category) {
                case MANUAL_EXPENSE, INVENTORY_PURCHASE -> CashTransactionType.EXPENSE;
                default -> CashTransactionType.INCOME;
            };
        }

        private RevenueReportRowResponse toResponse() {
            return new RevenueReportRowResponse(
                    category,
                    type,
                    transactionCount,
                    incomeAmount,
                    expenseAmount
            );
        }
    }
}
