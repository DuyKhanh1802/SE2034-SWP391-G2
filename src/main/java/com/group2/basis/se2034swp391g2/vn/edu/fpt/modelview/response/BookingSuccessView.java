package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class BookingSuccessView {

    private String bookingReference;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer totalRooms;

    private BigDecimal amount;

    private String bankName;
    private String accountNumber;
    private String accountName;

    private String transferContent;
    private String qrImageUrl;
}