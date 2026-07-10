package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;

public class InventoryReportSummaryResponse {

    private final long totalItemCount;
    private final BigDecimal totalStockValue;
    private final long lowStockCount;
    private final long outOfStockCount;
    private final BigDecimal totalReceivedQuantity;
    private final BigDecimal totalOutQuantity;
    private final long expiringSoonCount;
    private final long expiredCount;

    public InventoryReportSummaryResponse(long totalItemCount,
                                          BigDecimal totalStockValue,
                                          long lowStockCount,
                                          long outOfStockCount,
                                          BigDecimal totalReceivedQuantity,
                                          BigDecimal totalOutQuantity,
                                          long expiringSoonCount,
                                          long expiredCount) {
        this.totalItemCount = totalItemCount;
        this.totalStockValue = totalStockValue == null ? BigDecimal.ZERO : totalStockValue;
        this.lowStockCount = lowStockCount;
        this.outOfStockCount = outOfStockCount;
        this.totalReceivedQuantity = totalReceivedQuantity == null ? BigDecimal.ZERO : totalReceivedQuantity;
        this.totalOutQuantity = totalOutQuantity == null ? BigDecimal.ZERO : totalOutQuantity;
        this.expiringSoonCount = expiringSoonCount;
        this.expiredCount = expiredCount;
    }

    public long getTotalItemCount() {
        return totalItemCount;
    }

    public BigDecimal getTotalStockValue() {
        return totalStockValue;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public long getOutOfStockCount() {
        return outOfStockCount;
    }

    public BigDecimal getTotalReceivedQuantity() {
        return totalReceivedQuantity;
    }

    public BigDecimal getTotalOutQuantity() {
        return totalOutQuantity;
    }

    public long getExpiringSoonCount() {
        return expiringSoonCount;
    }

    public long getExpiredCount() {
        return expiredCount;
    }
}