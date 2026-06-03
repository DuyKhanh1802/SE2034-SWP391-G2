package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DiscountType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class PromotionResponse {

    private Long id;

    private String code;

    private String name;

    private DiscountType discountType;

    private BigDecimal discountValue;

    private BigDecimal maxDiscount;

    private Integer usageLimit;

    private Integer usageCount;

    private Instant validFrom;

    private Instant validTo;

    private Boolean isActive;

    private Instant createdAt;

    private String status;
}