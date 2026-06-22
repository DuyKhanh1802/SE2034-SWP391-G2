package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Data
public class RoomVariantDetail {
    private Long variantId;

    private String variantName;

    private String roomTypeName;

    private String viewType;

    private Integer capacity;

    private Integer roomSize;

    private String description;

    private Boolean allowExtraBed;

    private Integer maxExtraBeds;

    private BigDecimal extraBedPrice;

    private String extraBedNote;

    private Integer minFloor;

    private Integer maxFloor;

    private String primaryImageUrl;

    private List<String> imageUrls = new ArrayList<>();

    private String bedSummary;

    private List<String> amenities = new ArrayList<>();

    private List<String> service = new ArrayList<>();

    private Integer availableRooms;



}
