package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_categories")
public class InventoryCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_category_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100,
            columnDefinition = "NVARCHAR(100)")
    private String name;

}
