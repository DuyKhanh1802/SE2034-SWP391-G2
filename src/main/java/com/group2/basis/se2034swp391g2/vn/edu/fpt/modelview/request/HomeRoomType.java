package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomTypeName;
import org.apache.logging.log4j.message.StringFormattedMessage;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeRoomType {

    private Long id;
    private RoomTypeName name;
    private BigDecimal basePrice;
    private Integer capacity;
    private String description;
    private String imgUrl;



}
