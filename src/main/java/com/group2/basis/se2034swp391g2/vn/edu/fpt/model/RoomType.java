package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_type_id")
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "base_price", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal basePrice;

    // Sức chứa của phòng
    @Column(name = "capacity", nullable = false)
    private Integer capacity = 2;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults = 2;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren = 0;

    @Column(name = "room_size")
    private Integer roomSize;

    @Column(name = "description", length = 1000)
    private String description;

    // Chính sách kê thêm giường
    @Column(name = "allow_extra_bed", nullable = false)
    private Boolean allowExtraBed = false;

    @Column(name = "max_extra_beds", nullable = false)
    private Integer maxExtraBeds = 0;

    @Column(name = "extra_bed_price", precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal extraBedPrice;

    @Column(name = "extra_bed_note", length = 500)
    private String extraBedNote;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "roomType",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RoomTypeBed> beds = new HashSet<>();

    @OneToMany(
            mappedBy = "roomType",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RoomTypeAmenity> amenities = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.isDeleted == null) {
            this.isDeleted = false;
        }

        if (this.capacity == null) {
            this.capacity = 2;
        }

        if (this.maxAdults == null) {
            this.maxAdults = 2;
        }

        if (this.maxChildren == null) {
            this.maxChildren = 0;
        }

        if (this.allowExtraBed == null) {
            this.allowExtraBed = false;
        }

        if (this.maxExtraBeds == null) {
            this.maxExtraBeds = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}