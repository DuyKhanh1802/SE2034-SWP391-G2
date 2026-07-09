package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ServiceReportRowResponse {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final Long serviceId;
    private final String serviceName;
    private final String categoryName;
    private final Long soldQuantity;
    private final BigDecimal baseRevenue;
    private final BigDecimal serviceChargeAmount;
    private final BigDecimal vatAmount;
    private final BigDecimal totalRevenue;
    private final Instant lastOrderedAt;

    public ServiceReportRowResponse(Long serviceId,
                                    String serviceName,
                                    String categoryName,
                                    Long soldQuantity,
                                    BigDecimal baseRevenue,
                                    BigDecimal serviceChargeAmount,
                                    BigDecimal vatAmount,
                                    BigDecimal totalRevenue,
                                    Instant lastOrderedAt) {
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.categoryName = categoryName;
        this.soldQuantity = soldQuantity == null ? 0L : soldQuantity;
        this.baseRevenue = baseRevenue == null ? BigDecimal.ZERO : baseRevenue;
        this.serviceChargeAmount = serviceChargeAmount == null ? BigDecimal.ZERO : serviceChargeAmount;
        this.vatAmount = vatAmount == null ? BigDecimal.ZERO : vatAmount;
        this.totalRevenue = totalRevenue == null ? BigDecimal.ZERO : totalRevenue;
        this.lastOrderedAt = lastOrderedAt;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public Long getSoldQuantity() {
        return soldQuantity;
    }

    public BigDecimal getBaseRevenue() {
        return baseRevenue;
    }

    public BigDecimal getServiceChargeAmount() {
        return serviceChargeAmount;
    }

    public BigDecimal getVatAmount() {
        return vatAmount;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public Instant getLastOrderedAt() {
        return lastOrderedAt;
    }

    public LocalDateTime getLastOrderedAtVietnam() {
        if (lastOrderedAt == null) {
            return null;
        }
        return LocalDateTime.ofInstant(lastOrderedAt, APP_ZONE);
    }

    public BigDecimal getAverageUnitPrice() {
        if (soldQuantity == null || soldQuantity == 0) {
            return BigDecimal.ZERO;
        }

        return baseRevenue.divide(BigDecimal.valueOf(soldQuantity), 0, RoundingMode.HALF_UP);
    }
}