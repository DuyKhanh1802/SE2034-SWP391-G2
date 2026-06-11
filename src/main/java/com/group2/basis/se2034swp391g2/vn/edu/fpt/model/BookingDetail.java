package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ViewType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "booking_details")
public class BookingDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_detail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    // Có thể null nếu lúc booking chỉ chọn loại phòng,
    // đến check-in mới gán phòng thật.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "check_in_date")
    private LocalDate checkInDate;

    @Column(name = "check_out_date")
    private LocalDate checkOutDate;

    // Giá phòng / 1 đêm tại thời điểm đặt
    @Column(name = "price_per_night", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal pricePerNight = BigDecimal.ZERO;

    @Column(name = "num_nights", nullable = false)
    private Integer numNights = 1;

    // Tiền phòng chưa tính giường phụ
    // subtotal = pricePerNight * numNights
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "service_charge_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 0")
    private BigDecimal serviceChargeRate = BigDecimal.ZERO;

    @Column(name = "service_charge_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal serviceChargeAmount = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) default 0")
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "room_code", unique = true, length = 8)
    private String roomCode;

    @Column(name = "room_code_expires_at")
    private Instant roomCodeExpiresAt;

    // Nếu khách chọn view khi đặt phòng thì giữ trường này.
    // Ví dụ: CITY_VIEW, GARDEN_VIEW.
    @Enumerated(EnumType.STRING)
    @Column(name = "view_type", length = 30)
    private ViewType viewType;

    // Số người lớn ở riêng phòng này
    @Column(name = "num_adults", nullable = false)
    private Integer numAdults = 1;

    // Số trẻ em ở riêng phòng này
    @Column(name = "num_children", nullable = false)
    private Integer numChildren = 0;

    // Lưu tuổi trẻ em dạng chuỗi, ví dụ: "3,7"
    @Column(name = "child_ages", length = 100)
    private String childAges;

    // ==============================
    // THÊM: thông tin giường phụ
    // ==============================

    // THÊM: booking detail này chọn mấy giường phụ
    // Ví dụ: 0 = không chọn, 1 = thêm 1 giường phụ
    @Column(name = "extra_bed_count", nullable = false)
    private Integer extraBedCount = 0;

    // THÊM: giá 1 giường phụ / 1 đêm tại thời điểm đặt
    // Cần lưu lại để sau này RoomType đổi giá thì booking cũ không bị sai
    @Column(name = "extra_bed_price", precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal extraBedPrice = BigDecimal.ZERO;

    // THÊM: tổng tiền giường phụ của phòng này
    // extraBedTotal = extraBedCount * extraBedPrice * numNights
    @Column(name = "extra_bed_total", precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal extraBedTotal = BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        // SỬA NHẸ: set default để tránh null khi insert

        if (this.pricePerNight == null) {
            this.pricePerNight = BigDecimal.ZERO;
        }

        if (this.numNights == null) {
            this.numNights = 1;
        }

        if (this.subtotal == null) {
            this.subtotal = BigDecimal.ZERO;
        }

        if (this.serviceChargeRate == null) {
            this.serviceChargeRate = BigDecimal.ZERO;
        }

        if (this.serviceChargeAmount == null) {
            this.serviceChargeAmount = BigDecimal.ZERO;
        }

        if (this.vatRate == null) {
            this.vatRate = BigDecimal.ZERO;
        }

        if (this.vatAmount == null) {
            this.vatAmount = BigDecimal.ZERO;
        }

        if (this.totalAmount == null) {
            this.totalAmount = this.subtotal;
        }

        if (this.numAdults == null) {
            this.numAdults = 1;
        }

        if (this.numChildren == null) {
            this.numChildren = 0;
        }

        // THÊM: default cho giường phụ
        if (this.extraBedCount == null) {
            this.extraBedCount = 0;
        }

        if (this.extraBedPrice == null) {
            this.extraBedPrice = BigDecimal.ZERO;
        }

        if (this.extraBedTotal == null) {
            this.extraBedTotal = BigDecimal.ZERO;
        }
    }
}
