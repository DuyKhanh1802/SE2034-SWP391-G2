package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PromotionProjection {

    Long getPromotionId();

    String getCode();

    String getName();

    String getDescription();

    BigDecimal getDiscountAmount();

    Integer getUsageCount();

    Integer getUsageLimit();

    LocalDateTime getValidFrom();

    LocalDateTime getValidTo();

    Boolean getFeatured();

    String getImageUrl();

    Boolean getShowOnHomepage();
}