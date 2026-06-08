package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    private Long bookingId;

    private String bookingReference;

    private String guestName;

    private String roomNumber;

    private RoomTypeName roomTypeName;

    private String roomCode;

    private Instant roomCodeExpiresAt;

    private LocalDate checkInDate;

    private LocalDate checkOutDate;
}