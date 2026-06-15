package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;

public interface GuestRoomVariantProjection {

    Long getVariantId();

    String getVariantName();

    String getRoomTypeName();

    String getViewType();

    Integer getCapacity();

    Integer getMaxAdults();

    Integer getMaxChildren();

    BigDecimal getRoomSize();

    String getDescription();

    BigDecimal getPricePerNight();

    String getPrimaryImageUrl();

    String getImageUrls();

    Integer getTotalImages();

    String getBedSummary();

    String getAmenitySummary();

    String getServiceSummary();

    Integer getAvailableRooms();
}