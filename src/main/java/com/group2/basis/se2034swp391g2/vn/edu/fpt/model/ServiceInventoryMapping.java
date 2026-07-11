package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "service_inventory_mappings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_service_inventory_mapping",
                        columnNames = {"service_id", "inventory_item_id"}
                )
        }
)
public class ServiceInventoryMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    private InventoryItem item;

    @Column(name = "quantity_per_use", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityPerUse;
}
