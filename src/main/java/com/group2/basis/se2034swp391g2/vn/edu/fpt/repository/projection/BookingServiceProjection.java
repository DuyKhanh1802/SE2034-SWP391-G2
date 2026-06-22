package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;

public interface BookingServiceProjection {
    Long getServiceId();

    String getName();

    String getDescription();

    BigDecimal getPrice();

    Long getCategoryId();

    String getCategoryName();

    String getImageUrl();
}
