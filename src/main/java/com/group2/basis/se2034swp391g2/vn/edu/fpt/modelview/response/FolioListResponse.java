package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FolioListResponse {
    private Long bookingId;
    private Long bookingDetailId;
    private String bookingReference;
    private String guestName;
    private String roomSummary;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String bookingStatus;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String paymentStatus;
    private String paymentStatusLabel;
}
