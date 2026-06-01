package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "countries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "country_code", nullable = false, unique = true, length = 2)
    private String countryCode;

    @Column(name = "country_name", nullable = false, length = 100)
    @org.hibernate.annotations.Nationalized
    private String countryName;
}
