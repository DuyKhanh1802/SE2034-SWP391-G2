package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;

public interface RoomTypeProjection {

    Long getId();

    String getName();

    BigDecimal getBasePrice();

    Integer getCapacity();

    Integer getMaxAdults();

    Integer getMaxChildren();

    Integer getMaxExtraBeds();

    String getDescription();

    String getRoomSize();

    Boolean getAllowExtraBed();

    BigDecimal getExtraBedPrice();

    String getExtraBedNote();

    String getImageUrl();
}
