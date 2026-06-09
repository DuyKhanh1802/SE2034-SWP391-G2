package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ServiceCategoryType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.model.ServiceCategory;

import java.math.BigDecimal;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeService {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private ServiceCategoryType categoryType;
    private String categoryName;
    private String imgUrl;


}
