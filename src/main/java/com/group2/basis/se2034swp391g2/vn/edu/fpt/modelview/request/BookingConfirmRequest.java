package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Data
public class BookingConfirmRequest {

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

    private List<SelectedRoomServiceRequest> roomServices = new ArrayList<>();
}