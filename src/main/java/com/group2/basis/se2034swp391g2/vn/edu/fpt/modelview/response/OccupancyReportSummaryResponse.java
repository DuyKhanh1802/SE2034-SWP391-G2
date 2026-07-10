package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OccupancyReportSummaryResponse {
    private final long totalRooms;
    private final long availableRoomNights;
    private final long occupiedRoomNights;
    private final long bookingCount;
    private final String bestPerformingVariant;

    public OccupancyReportSummaryResponse(long totalRooms,
                                          long availableRoomNights,
                                          long occupiedRoomNights,
                                          long bookingCount,
                                          String bestPerformingVariant) {
        this.totalRooms = totalRooms;
        this.availableRoomNights = availableRoomNights;
        this.occupiedRoomNights = occupiedRoomNights;
        this.bookingCount = bookingCount;
        this.bestPerformingVariant = bestPerformingVariant;
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

    public String getBestPerformingVariant() {
        return bestPerformingVariant;
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
