package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutDetailResponse {
    private Long bookingId;
    private Long bookingDetailId;
    private String bookingReference;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private String roomNumber;
    private String roomTypeName;
    private String variantName;
    private Integer roomCount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Instant actualCheckinAt;
    private Instant actualCheckoutAt;
    private String bookingStatus;
    private String bookingStatusLabel;
    private BigDecimal roomSubtotal;
    private BigDecimal serviceSubtotal;
    private BigDecimal serviceChargeTotal;
    private BigDecimal vatTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal refundAmount;
    private BigDecimal balanceAmount;
    private String settlementType;
    private String paymentStatusLabel;
    private boolean canCheckout;
    private String blockReason;

}
