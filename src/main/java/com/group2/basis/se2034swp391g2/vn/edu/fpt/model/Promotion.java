package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    private Long id;

    /*
     * Promotion code do hệ thống tự sinh.
     * Manager không nhập tay.
     * Ví dụ: VH-202606-A8K2
     */
    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /*
     * Mô tả ngắn để hiển thị ở homepage / special offers.
     */
    @Column(name = "description", length = 300)
    private String description;

    /*
     * Nghiệp vụ mới chỉ dùng giảm số tiền cố định.
     */
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "usage_limit", nullable = false)
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to", nullable = false)
    private Instant validTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /*
     * Có hiển thị promotion này ở homepage không.
     */
    @Column(name = "show_on_homepage", nullable = false)
    private Boolean showOnHomepage = false;

    /*
     * Có dùng promotion này làm banner nổi bật không.
     */
    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    /*
     * Link ảnh promotion.
     * Nếu dùng Cloudinary thì lưu secure_url ở đây.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /*
     * Nếu dùng Cloudinary thì lưu public_id để sau này update/delete ảnh.
     */
    @Column(name = "image_public_id", length = 200)
    private String imagePublicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.usageCount == null) {
            this.usageCount = 0;
        }

        if (this.isActive == null) {
            this.isActive = true;
        }

        if (this.showOnHomepage == null) {
            this.showOnHomepage = false;
        }

        if (this.featured == null) {
            this.featured = false;
        }
    }
}