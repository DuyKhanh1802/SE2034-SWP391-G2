package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class GuestMyBookingView {

    private Long bookingId;
    private Long bookingDetailId;

    private String bookingReference;
    private String bookingStatus;
    private String depositStatus;

    private String guestName;
    private String guestEmail;
    private String guestPhone;

    private Instant bookingDate;

    private String roomCode;
    private String roomNumber;
    private Integer floor;
    private String roomStatus;

    private String roomTypeName;
    private String variantName;
    private String roomTypeDescription;
    private String viewType;

    private Integer roomSize;
    private Integer capacity;
    private Integer maxAdults;
    private Integer maxChildren;

    private Boolean allowExtraBed;
    private Integer maxExtraBeds;
    private BigDecimal extraBedPrice;
    private String extraBedNote;

    private String roomImageUrl;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numNights;

    private Integer numAdults;
    private Integer numChildren;
    private Integer totalRooms;

    private BigDecimal pricePerNight;
    private BigDecimal roomSubtotal;
    private BigDecimal serviceChargeAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;

    private String serviceSummary;
    private String specialRequests;

    private String bedSummary;

    private List<GuestAmenityView> amenities = new ArrayList<>();
    private List<GuestBedView> beds = new ArrayList<>();
    private List<GuestSelectedServiceView> selectedServices = new ArrayList<>();
    private List<GuestFolioTransactionView> folioTransactions = new ArrayList<>();

    private BigDecimal folioTotalCharge = BigDecimal.ZERO;
    private BigDecimal folioPaidAmount = BigDecimal.ZERO;
    private BigDecimal folioRemainingBalance = BigDecimal.ZERO;
}