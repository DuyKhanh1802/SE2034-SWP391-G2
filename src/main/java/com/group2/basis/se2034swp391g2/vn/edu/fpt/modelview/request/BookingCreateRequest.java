package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class BookingCreateRequest {

    private String fullName;

    private String phoneNumber;

    private String email;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    private Integer adults;

    private Integer children;

    private List<Long> roomIds;

    private String notes;

    // create-only hoặc create-check-in
    private String action;
}