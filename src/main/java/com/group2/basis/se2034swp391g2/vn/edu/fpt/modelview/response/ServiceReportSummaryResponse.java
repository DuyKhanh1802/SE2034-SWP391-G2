package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;

public class ServiceReportSummaryResponse {
    private final long totalServiceCount;
    private final long totalSoldQuantity;
    private final BigDecimal totalBaseRevenue;
    private final BigDecimal totalServiceCharge;
    private final BigDecimal totalVat;
    private final BigDecimal totalRevenue;
    private final String bestSellingService;
    private final String highestRevenueService;

    public ServiceReportSummaryResponse(long totalServiceCount,
                                        long totalSoldQuantity,
                                        BigDecimal totalBaseRevenue,
                                        BigDecimal totalServiceCharge,
                                        BigDecimal totalVat,
                                        BigDecimal totalRevenue,
                                        String bestSellingService,
                                        String highestRevenueService) {
        this.totalServiceCount = totalServiceCount;
        this.totalSoldQuantity = totalSoldQuantity;
        this.totalBaseRevenue = totalBaseRevenue == null ? BigDecimal.ZERO : totalBaseRevenue;
        this.totalServiceCharge = totalServiceCharge == null ? BigDecimal.ZERO : totalServiceCharge;
        this.totalVat = totalVat == null ? BigDecimal.ZERO : totalVat;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.bestSellingService = bestSellingService;
        this.highestRevenueService = highestRevenueService;
    }

    public long getTotalServiceCount() {
        return totalServiceCount;
    }

    public long getTotalSoldQuantity() {
        return totalSoldQuantity;
    }

    public BigDecimal getTotalBaseRevenue() {
        return totalBaseRevenue;
    }

    public BigDecimal getTotalServiceCharge() {
        return totalServiceCharge;
    }

    public BigDecimal getTotalVat() {
        return totalVat;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public String getBestSellingService() {
        return bestSellingService;
    }

    public String getHighestRevenueService() {
        return highestRevenueService;
    }
}