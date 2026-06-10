package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomTypeName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String imageUrl;

    public String getDisplayName() {
        if (name == null) {
            return "";
        }

        String text = name.name().replace("_", " ").toLowerCase();
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }
}