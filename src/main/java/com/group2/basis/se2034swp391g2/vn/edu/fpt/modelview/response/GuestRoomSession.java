package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.*;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuestRoomSession implements Serializable {

    private Long bookingId;
    private Long bookingDetailId;
    private Long roomId;
    private Long guestId;

    private String roomNumber;
    private String guestName;
    private String guestEmail;
    private String bookingReference;
    private String avatarUrl;

    private Instant roomCodeExpiresAt;
}