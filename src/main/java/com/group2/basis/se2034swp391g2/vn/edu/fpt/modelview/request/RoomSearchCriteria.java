package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.*;

import java.time.LocalDate;

@Data
public class RoomSearchCriteria {
    private final Long roomTypeId;
    private final String viewType;
    private final String sort;

    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    private final Integer adults;
    private final Integer children;
    private final Integer roomCount;

    private final String roomGuests;
    private final String warningMessage;
}
