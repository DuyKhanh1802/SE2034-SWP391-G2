package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "room_type_variant_services",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"variant_id", "service_id"})
        }
)
public class RoomTypeVariantService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_service_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private RoomTypeVariant variant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "included_type", nullable = false, length = 30)
    private String includedType = "INCLUDED";

    @Column(name = "note", length = 300, columnDefinition = "NVARCHAR(300)")
    private String note;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}