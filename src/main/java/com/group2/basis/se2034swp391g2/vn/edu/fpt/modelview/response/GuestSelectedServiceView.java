package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import java.math.BigDecimal;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.FolioItemStatus;
import lombok.*;

@Data
public class GuestSelectedServiceView {
    private Long folioItemId;
    private Long serviceId;
    private String serviceName;
    private String categoryName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private FolioItemStatus status;
    private boolean editable;
}
