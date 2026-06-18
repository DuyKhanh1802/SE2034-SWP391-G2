package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;

public interface RoomVariantDetailProjection {
    Long getVariantId();

    String getVariantName();

    String getRoomTypeName();

    String getViewType();

    Integer getCapacity();

    Integer getRoomSize();

    String getDescription();

    Boolean getAllowExtraBed();

    Integer getMaxExtraBeds();

    BigDecimal getExtraBedPrice();

    String getExtraBedNote();

    Integer getMinFloor();

    Integer getMaxFloor();

    String getPrimaryImageUrl();

    String getImageUrls();

    Integer getTotalImages();

    String getBedSummary();

    String getAmenitySummary();

    String getServiceSummary();

    Integer getAvailableRooms();
}
