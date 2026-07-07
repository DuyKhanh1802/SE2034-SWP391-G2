package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.*;

@Data
public class GuestResult {
    private final Integer adults;
    private final Integer children;
    private final Integer roomCount;
    private final String roomGuests;
    private final String warningMessage;
}
