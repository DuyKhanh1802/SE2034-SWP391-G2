package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "countries")
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long id;

    @Column(name = "country_name", nullable = false, unique = true, length = 100)
    private String countryName;

    @Column(name = "country_code", nullable = false, unique = true, length = 2)
    private String countryCode;

    @Column(name = "phone_code", length = 10)
    private String phoneCode;
}
