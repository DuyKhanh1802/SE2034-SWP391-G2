package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ReceptionistDashboardView(
        LocalDate today,

        long todayArrivals,
        long stayingGuests,
        long todayCheckOuts,
        long pendingBookings,

        List<CheckInRow> checkInRows,
        List<CheckOutRow> checkOutRows,
        List<RoomRow> roomRows,
        List<PendingBookingRow> pendingBookingRows,
        List<RoomMoveRow> roomMoveRows
) {

    public record CheckInRow(
            Long bookingId,
            String bookingReference,
            String guestName,
            String guestPhone,
            String roomDisplay,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Integer numAdults,
            Integer numChildren,
            String depositStatus
    ) {
    }

    public record CheckOutRow(
            Long bookingId,
            String roomNumber,
            String guestName,
            LocalDate checkOutDate,
            BigDecimal totalAmount,
            BigDecimal paidAmount,
            BigDecimal remainingAmount
    ) {
    }

    public record RoomRow(
            Long roomId,
            String roomNumber,
            String roomType,
            String variantName,
            Integer floor,
            String status
    ) {
    }

    public record PendingBookingRow(
            Long bookingId,
            String bookingReference,
            String guestName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String status
    ) {
    }

    public record RoomMoveRow(
            Long bookingId,
            String guestName,
            String oldRoomNumber,
            String newRoomNumber,
            String reasonType,
            BigDecimal extraChargeAmount,
            Instant movedAt
    ) {
    }
}