package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
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
public class ViewBookingDetailResponse {

    private Long bookingId;
    private String bookingReference;

    private BookingStatus bookingStatus;
    private DepositStatus depositStatus;

    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String gender;

    private Integer birthYear;

    private String countryName;
    private String identityType;
    private String identityNumber;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Long nights;
    private Integer adults;
    private Integer children;
    private String specialRequests;

    private BigDecimal roomTotal;
    private BigDecimal depositPaid;
    private BigDecimal remainingEstimate;
    private BigDecimal vatTotal;
    private BigDecimal grandTotal;
    private BigDecimal depositRequired;

    private BigDecimal serviceSubtotal;
    private BigDecimal serviceChargeTotal;

    private Instant createdAt;

    private String cancelReason;
    private Instant cancelledAt;
    private String cancelledByName;

    private String promotionCode;
    private String promotionName;
    private BigDecimal discountAmount;
    private BigDecimal totalBeforeDiscount;
    private LocalDate passportExpiryDate;

    @Builder.Default
    private List<RoomLine> rooms = new ArrayList<>();

    @Builder.Default
    private List<ServiceLine> services = new ArrayList<>();

    @Builder.Default
    private List<PaymentLine> payments = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoomLine {
        private Long bookingDetailId;
        private String roomNumber;

        // Xóa birthYear ở đây vì năm sinh là thông tin guest, không phải thông tin room
        private String roomTypeName;
        private String variantName;
        private String viewType;

        private Integer extraBedCount;
        private BigDecimal extraBedTotal;
        private BigDecimal totalAmount;

        private BigDecimal pricePerNight;
        private Integer numNights;
        private BigDecimal subtotal;

        private String roomCode;
        private Instant roomCodeExpiresAt;
        private LocalDate checkOutDate;
        private Long variantId;
        private String stayStatus;
        private String stayStatusLabel;
        private Integer numAdults;
        private Integer numChildren;
        private Integer guestCount;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceLine {
        private Long folioItemId;
        private String serviceName;
        private String itemType;
        private String serviceStatus;
        private String serviceStatusLabel;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private Instant postedAt;
        private String postedBy;
        private Long bookingDetailId;
        private String roomNumber;
    }
}
