package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionCategory;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.CashTransactionType;

import java.math.BigDecimal;

public class RevenueReportRowResponse {
    private final CashTransactionCategory category;
    private final CashTransactionType type;
    private final long transactionCount;
    private final BigDecimal incomeAmount;
    private final BigDecimal expenseAmount;

    public RevenueReportRowResponse(CashTransactionCategory category,
                                    CashTransactionType type,
                                    long transactionCount,
                                    BigDecimal incomeAmount,
                                    BigDecimal expenseAmount) {
        this.category = category;
        this.type = type;
        this.transactionCount = transactionCount;
        this.incomeAmount = incomeAmount == null ? BigDecimal.ZERO : incomeAmount;
        this.expenseAmount = expenseAmount == null ? BigDecimal.ZERO : expenseAmount;
    }

    public CashTransactionCategory getCategory() {
        return category;
    }

    public CashTransactionType getType() {
        return type;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public BigDecimal getIncomeAmount() {
        return incomeAmount;
    }

    public BigDecimal getExpenseAmount() {
        return expenseAmount;
    }

    public BigDecimal getNetAmount() {
        return incomeAmount.subtract(expenseAmount);
    }
}
