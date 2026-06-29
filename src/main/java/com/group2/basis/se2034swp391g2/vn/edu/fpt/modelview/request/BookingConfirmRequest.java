package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingConfirmRequest {

    private String bookingReference;

    private String variantIds;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    private Integer adults;
    private Integer children;
    private Integer roomCount;
    private String roomGuests;
    private String promoCode;

    private String guestFirstName;
    private String guestLastName;
    private String guestPhone;
    private String guestEmail;
    private String specialRequests;

    private Boolean paymentAcknowledged;

    private List<SelectedRoomServiceRequest> roomServices = new ArrayList<>();
}