package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMoveOptionResponse {
    private Long roomId;
    private String roomNumber;
    private Integer floor;
    private Long variantId;
    private String roomTypeName;
    private String variantName;
    private String viewType;
    private BigDecimal pricePerNight;
    private BigDecimal priceDifferencePerNight;
    private Boolean upgrade;
}