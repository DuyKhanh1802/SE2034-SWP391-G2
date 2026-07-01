package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.Data;

@Data
public class GuestIncludedServiceView {
    private Long serviceId;
    private String serviceName;
    private String categoryName;
    private Integer quantity;
    private String includedType;
    private String note;
}