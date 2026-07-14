package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;

public class RevenueReportSummaryResponse {
    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal netAmount;
    private final long transactionCount;
    private final String highestIncomeCategory;

    public RevenueReportSummaryResponse(BigDecimal totalIncome,
                                        BigDecimal totalExpense,
                                        long transactionCount,
                                        String highestIncomeCategory) {
        this.totalIncome = totalIncome == null ? BigDecimal.ZERO : totalIncome;
        this.totalExpense = totalExpense == null ? BigDecimal.ZERO : totalExpense;
        this.netAmount = this.totalIncome.subtract(this.totalExpense);
        this.transactionCount = transactionCount;
        this.highestIncomeCategory = highestIncomeCategory;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getNetAmount() {
        return netAmount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public String getHighestIncomeCategory() {
        return highestIncomeCategory;
    }
}
