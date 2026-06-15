package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long roomId;

    private String roomNumber;

    private Long variantId;

    private String roomTypeName;

    private BigDecimal basePrice;

    private String viewType;

    private Integer capacity;

    private Integer maxAdults;

    private Integer maxChildren;

    private Boolean allowExtraBed;

    private Integer maxExtraBeds;

    private BigDecimal extraBedPrice;

    private String extraBedNote;

    private List<String> includedServices = new ArrayList<>();

    public RoomResponse(Long roomId,
                        String roomNumber,
                        String roomTypeName,
                        BigDecimal basePrice) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.roomTypeName = roomTypeName;
        this.basePrice = basePrice;
    }

    public RoomResponse(Long roomId,
                        String roomNumber,
                        Long variantId,
                        String roomTypeName,
                        BigDecimal basePrice,
                        String viewType,
                        Integer capacity,
                        Integer maxAdults,
                        Integer maxChildren,
                        Boolean allowExtraBed,
                        Integer maxExtraBeds,
                        BigDecimal extraBedPrice,
                        String extraBedNote) {
        this.roomId = roomId;
        this.roomNumber = roomNumber;
        this.variantId = variantId;
        this.roomTypeName = roomTypeName;
        this.basePrice = basePrice;
        this.viewType = viewType;
        this.capacity = capacity;
        this.maxAdults = maxAdults;
        this.maxChildren = maxChildren;
        this.allowExtraBed = allowExtraBed;
        this.maxExtraBeds = maxExtraBeds;
        this.extraBedPrice = extraBedPrice;
        this.extraBedNote = extraBedNote;
        this.includedServices = new ArrayList<>();
    }
}