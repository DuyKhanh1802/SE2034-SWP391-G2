package com.group2.basis.se2034swp391g2.vn.edu.fpt.modelview.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Long categoryId;

    private Boolean isAvailable = true;

    private MultipartFile imageFile;

    private String currentImageUrl;
}