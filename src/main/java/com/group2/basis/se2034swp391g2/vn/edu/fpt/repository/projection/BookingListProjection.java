package com.group2.basis.se2034swp391g2.vn.edu.fpt.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BookingListProjection {
    Long getBookingId();
    String getBookingReference();
    String getGuestName();
    String getVariantName();
    String getRoomNumber();
    LocalDate getCheckInDate();
    LocalDate getCheckOutDate();
    String getStatus();
    BigDecimal getTotalAmount();
}