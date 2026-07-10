package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    private List<RoomLine> rooms = new ArrayList<>();

    @Builder.Default
    private List<FolioLine> folioLines = new ArrayList<>();

    @Builder.Default
    private List<PaymentLine> payments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomLine {
        private Long bookingDetailId;
        private String roomNumber;
        private String roomTypeName;
        private String variantName;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolioLine {
        private String itemType;
        private String description;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String statusLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentLine {
        private String transactionRef;
        private String paymentType;
        private String method;
        private String status;
        private BigDecimal amount;
        private Instant paidAt;
    }
}
