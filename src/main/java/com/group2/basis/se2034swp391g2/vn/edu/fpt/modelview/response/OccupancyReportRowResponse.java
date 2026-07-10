package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OccupancyReportRowResponse {
    private final Long variantId;
    private final String roomTypeName;
    private final String variantName;
    private final long totalRooms;
    private final long availableRoomNights;
    private final long occupiedRoomNights;
    private final long bookingCount;

    public OccupancyReportRowResponse(Long variantId,
                                      String roomTypeName,
                                      String variantName,
                                      long totalRooms,
                                      long availableRoomNights,
                                      long occupiedRoomNights,
                                      long bookingCount) {
        this.variantId = variantId;
        this.roomTypeName = roomTypeName;
        this.variantName = variantName;
        this.totalRooms = totalRooms;
        this.availableRoomNights = availableRoomNights;
        this.occupiedRoomNights = occupiedRoomNights;
        this.bookingCount = bookingCount;
    }

    public Long getVariantId() {
        return variantId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public String getVariantName() {
        return variantName;
    }

    public long getTotalRooms() {
        return totalRooms;
    }

    public long getAvailableRoomNights() {
        return availableRoomNights;
    }

    public long getOccupiedRoomNights() {
        return occupiedRoomNights;
    }

    public long getBookingCount() {
        return bookingCount;
    }

    public BigDecimal getOccupancyRate() {
        if (availableRoomNights == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(occupiedRoomNights)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(availableRoomNights), 1, RoundingMode.HALF_UP);
    }
}
