package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class BookingConfirmView {

    private String bookingReference;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private long nights;

    private Integer adults;
    private Integer children;
    private Integer roomCount;
    private String promoCode;

    private List<RoomLine> rooms = new ArrayList<>();

    private PriceSummary priceSummary;

    private BankTransferInfo bankTransferInfo;

    @Data
    @NoArgsConstructor
    public static class RoomLine {
        private Integer roomIndex;
        private Long variantId;
        private String roomTypeName;
        private String variantName;
        private String viewType;

        private String imageUrl;

        private Integer adults;
        private Integer children;

        private BigDecimal pricePerNight;
        private BigDecimal roomSubtotal;

        private String includedServiceSummary;

        private List<ServiceLine> addOnServices = new ArrayList<>();
        private BigDecimal addOnServiceSubtotal;
        private String addOnServiceSummary;
    }

    @Data
    @NoArgsConstructor
    public static class ServiceLine {
        private Long serviceId;
        private String serviceName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }

    @Data
    @NoArgsConstructor
    public static class PriceSummary {
        private BigDecimal roomSubtotal;
        private BigDecimal serviceSubtotal;
        private BigDecimal subtotalBeforeFees;
        private BigDecimal serviceChargeTotal;
        private BigDecimal vatTotal;
        private BigDecimal discountAmount;
        private BigDecimal grandTotal;
    }

    @Data
    @NoArgsConstructor
    public static class BankTransferInfo {
        private String bankName;
        private String accountNumber;
        private String accountName;
        private String transferContent;
        private BigDecimal amount;
        private String qrImageUrl;
    }
}