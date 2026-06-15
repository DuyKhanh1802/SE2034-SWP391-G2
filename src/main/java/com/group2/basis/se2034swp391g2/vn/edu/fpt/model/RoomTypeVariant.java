package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
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
@Table(name = "room_type_variants")
public class RoomTypeVariant {

    @OneToMany(
            mappedBy = "variant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RoomTypeVariantService> includedServices = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "variant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "variant_name", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String variantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "view_type", nullable = false, length = 30)
    private ViewType viewType;

    @Column(name = "price_per_night", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal pricePerNight;

    @Column(name = "room_size")
    private Integer roomSize;

    @Column(name = "capacity", nullable = false)
    private Integer capacity = 2;

    @Column(name = "max_adults", nullable = false)
    private Integer maxAdults = 2;

    @Column(name = "max_children", nullable = false)
    private Integer maxChildren = 0;

    @Column(name = "allow_extra_bed", nullable = false)
    private Boolean allowExtraBed = false;

    @Column(name = "max_extra_beds", nullable = false)
    private Integer maxExtraBeds = 0;

    @Column(name = "extra_bed_price", precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal extraBedPrice;

    @Column(name = "extra_bed_note", length = 500, columnDefinition = "NVARCHAR(500)")
    private String extraBedNote;

    @Column(name = "description", length = 1000, columnDefinition = "NVARCHAR(1000)")
    private String description;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(
            mappedBy = "variant",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<RoomTypeVariantBed> beds = new HashSet<>();

    @OneToMany(mappedBy = "variant")
    private Set<Room> rooms = new HashSet<>();

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
        if (this.pricePerNight == null) {
            this.pricePerNight = BigDecimal.ZERO;
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
