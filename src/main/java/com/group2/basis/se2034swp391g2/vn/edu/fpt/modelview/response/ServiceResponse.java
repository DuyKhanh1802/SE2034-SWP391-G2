package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Boolean isAvailable;

    private Long categoryId;

    private String categoryName;

    private String imageUrl;
}