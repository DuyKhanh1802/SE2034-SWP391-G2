package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;

import java.math.BigDecimal;

public interface HomeServiceProjection {
    Long getId();

    String getName();

    String getDescription();

    BigDecimal getPrice();

    ServiceCategoryType getCategoryType();

    String getCategoryName();

    String getImageUrl();
}
