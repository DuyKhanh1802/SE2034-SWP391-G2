package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long roomId;

    private String roomNumber;

    private RoomTypeName roomTypeName;

    private BigDecimal basePrice;
}