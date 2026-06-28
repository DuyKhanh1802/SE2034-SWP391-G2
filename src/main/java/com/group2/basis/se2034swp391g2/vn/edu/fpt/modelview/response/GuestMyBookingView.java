package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;


import java.math.BigDecimal;
import java.time.LocalDate;


public class GuestMyBookingView {
    private Long bookingId;
    private Long getBookingId;


    private String bookingReference;
    private String bookingStatus;
    private String depositStatus;


    private String firstName;
    private String lastName;
    private String email;
    private String phone;


    private String roomCode;
    private String roomNumber;
    private String roomTypeName;
    private String variantName;


    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numNights;


    private Integer numAdults;
    private Integer numChildren;


    private BigDecimal pricePerNight;
    private BigDecimal roomSubtotal;
    private BigDecimal serviceChargeAmount;
    private BigDecimal varAmount;
    private BigDecimal totalAmount;


    private String serviceSummary;
    private String specialRequests;


}



