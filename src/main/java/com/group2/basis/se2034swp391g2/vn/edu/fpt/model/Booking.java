package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.BookingStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DepositStatus;
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
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @Column(name = "guest_first_name", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String guestFirstName;

    @Column(name = "guest_last_name", nullable = false, length = 50, columnDefinition = "NVARCHAR(50)")
    private String guestLastName;

    @Column(name = "guest_phone", nullable = false, length = 20)
    private String guestPhone;

    @Column(name = "guest_email", length = 150)
    private String guestEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private User guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    // Tổng số người lớn của cả booking
    @Column(name = "num_adults", nullable = false)
    private Integer numAdults;

    // Tổng số phòng trong booking
    @Column(name = "total_rooms", nullable = false)
    private Integer totalRooms = 1;

    // Tổng số trẻ em của cả booking
    @Column(name = "num_children", nullable = false)
    private Integer numChildren;

    @Column(name = "special_requests", length = 500, columnDefinition = "NVARCHAR(500)")
    private String specialRequests;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 20)
    private String bookingReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "deposit_status", nullable = false, length = 15)
    private DepositStatus depositStatus = DepositStatus.UNPAID;

    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING;

    // Tổng tiền cuối cùng của booking
    // Bao gồm tiền phòng + tiền giường phụ nếu có - giảm giá
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0)")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "room_subtotal", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal roomSubtotal = BigDecimal.ZERO;

    @Column(name = "service_subtotal", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal serviceSubtotal = BigDecimal.ZERO;

    @Column(name = "service_charge_total", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal serviceChargeTotal = BigDecimal.ZERO;

    @Column(name = "vat_total", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal vatTotal = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "amount_calculated_at")
    private Instant amountCalculatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancel_reason", length = 300, columnDefinition = "NVARCHAR(300)")
    private String cancelReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "actual_checkout_at")
    private Instant actualCheckoutAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        this.updatedAt = now;

        // SỬA NHẸ: set default để tránh null khi insert
        if (this.discountAmount == null) {
            this.discountAmount = BigDecimal.ZERO;
        }

        // SỬA NHẸ: nếu không truyền totalRooms thì mặc định 1 phòng
        if (this.totalRooms == null) {
            this.totalRooms = 1;
        }

        // SỬA NHẸ: nếu không có trẻ em thì mặc định 0
        if (this.numChildren == null) {
            this.numChildren = 0;
        }

        // SỬA NHẸ: mặc định chưa thanh toán cọc
        if (this.depositStatus == null) {
            this.depositStatus = DepositStatus.UNPAID;
        }
        if (this.depositAmount == null) {
            this.depositAmount = BigDecimal.ZERO;
        }

        // SỬA NHẸ: trạng thái booking ban đầu là PENDING
        if (this.status == null) {
            this.status = BookingStatus.PENDING;
        }

        // SỬA NHẸ: tổng tiền mặc định là 0
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }

        if (this.roomSubtotal == null) {
            this.roomSubtotal = BigDecimal.ZERO;
        }

        if (this.serviceSubtotal == null) {
            this.serviceSubtotal = BigDecimal.ZERO;
        }

        if (this.serviceChargeTotal == null) {
            this.serviceChargeTotal = BigDecimal.ZERO;
        }

        if (this.vatTotal == null) {
            this.vatTotal = BigDecimal.ZERO;
        }

        if (this.grandTotal == null) {
            this.grandTotal = this.totalAmount;
        }

        // SỬA NHẸ: mặc định chưa xóa mềm
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
