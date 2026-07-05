package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.*;

import java.time.LocalDate;

@Data
public class StayDateResult {
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;
    private final String warningMessage;
}
