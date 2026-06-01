package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "room_types",
        uniqueConstraints = @UniqueConstraint(name = "uq_room_types_name", columnNames = "name"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal basePrice;

    @Column(name = "capacity", nullable = false)
    private Byte capacity = 2;

    @Column(name = "description", length = 500)
    @Nationalized
    private String description;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
