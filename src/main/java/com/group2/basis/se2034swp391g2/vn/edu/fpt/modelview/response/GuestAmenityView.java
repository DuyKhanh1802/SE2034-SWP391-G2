package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;
import lombok.*;

@Data
public class GuestAmenityView {
    private Long amenityId;
    private String name;
    private String icon;
    private Boolean highlighted;
    private Integer sortOrder;

}
