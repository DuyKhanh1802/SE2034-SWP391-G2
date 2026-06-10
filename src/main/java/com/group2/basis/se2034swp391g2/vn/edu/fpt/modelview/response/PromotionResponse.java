package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/*
 * Response cho từng khuyến mãi hiển thị trên danh sách.
 */
@Getter
@Builder
public class PromotionResponse {

    private Long id;

    private String code;

    private String name;

    private String description;

    private BigDecimal discountAmount;

    private Integer usageLimit;

    private Integer usageCount;

    private Instant validFrom;

    private Instant validTo;

    private Boolean isActive;

    private Boolean showOnHomepage;

    private Boolean featured;

    private String imageUrl;

    private String imagePublicId;

    private Instant createdAt;

    private String status;

    private String displayStatus;
}