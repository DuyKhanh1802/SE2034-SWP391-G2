package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BookingCompleteResult {
    private Long bookingId;
    private String bookingReference;
}