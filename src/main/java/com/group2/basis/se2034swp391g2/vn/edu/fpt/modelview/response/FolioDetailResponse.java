package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolioDetailResponse {
    private Long bookingId;
    private String bookingReference;
    private String guestName;
    private String guestPhone;
    private String guestEmail;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String bookingStatus;
    private BigDecimal roomSubtotal;
    private BigDecimal serviceSubtotal;
    private BigDecimal serviceChargeTotal;
    private BigDecimal vatTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private String paymentStatus;
    private String paymentStatusLabel;
    private boolean editable;

    @Builder.Default
    private List<RoomLine> rooms = new ArrayList<>();

    @Builder.Default
    private List<FolioLine> roomCharges = new ArrayList<>();

    @Builder.Default
    private List<FolioLine> serviceCharges = new ArrayList<>();

    @Builder.Default
    private List<FolioLine> adjustments = new ArrayList<>();

    @Builder.Default
    private List<FolioLine> invoiceCharges = new ArrayList<>();

    @Builder.Default
    private List<PaymentLine> payments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomLine {
        private Long bookingDetailId;
        private String roomNumber;
        private String roomName;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FolioLine {
        private Long folioItemId;
        private Long bookingDetailId;
        private String roomNumber;
        private String description;
        private String itemType;
        private String statusLabel;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal baseAmount;
        private BigDecimal serviceChargeAmount;
        private BigDecimal vatAmount;
        private BigDecimal totalAmount;
        private Instant postedAt;
        private String adjustmentReason;
        private boolean voidable;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentLine {
        private String paymentType;
        private String method;
        private BigDecimal amount;
        private String status;
        private Instant paidAt;
        private String transactionRef;
    }
}
