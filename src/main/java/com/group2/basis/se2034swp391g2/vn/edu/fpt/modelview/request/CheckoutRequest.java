package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.PaymentMethod;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutRequest {
    private BigDecimal paymentAmount = BigDecimal.ZERO;
    private PaymentMethod paymentMethod;
    private BigDecimal refundAmount = BigDecimal.ZERO;
    private PaymentMethod refundMethod;
    private RoomStatus roomStatusAfterCheckout = RoomStatus.MAINTENANCE;
    private String note;
}
