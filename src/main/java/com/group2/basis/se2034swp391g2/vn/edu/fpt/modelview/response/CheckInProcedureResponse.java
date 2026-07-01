package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInProcedureResponse {

    private Long bookingId;
    private String bookingReference;
    private String guestName;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    private String specialRequests;

    private BigDecimal roomTotal;
    private String status;

    private Integer adults;
    private Integer children;

    private String identityType;
    private String identityNumber;

    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
}