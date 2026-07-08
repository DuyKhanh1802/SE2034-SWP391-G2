package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveFeePolicy;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomMoveReason;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoomStatus;
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
@Table(name = "room_move_logs")
public class RoomMoveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_move_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_detail_id", nullable = false)
    private BookingDetail bookingDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_room_id", nullable = false)
    private Room oldRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_room_id", nullable = false)
    private Room newRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, length = 30)
    private RoomMoveReason reasonType;

    @Column(name = "reason_note", length = 500, columnDefinition = "NVARCHAR(500)")
    private String reasonNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_policy", nullable = false, length = 30)
    private RoomMoveFeePolicy feePolicy;

    @Column(name = "price_difference_per_night", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal priceDifferencePerNight = BigDecimal.ZERO;

    @Column(name = "charged_nights", nullable = false)
    private Integer chargedNights = 0;

    @Column(name = "extra_charge_amount", nullable = false, precision = 15, scale = 0, columnDefinition = "numeric(15,0) default 0")
    private BigDecimal extraChargeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_room_status_after_move", nullable = false, length = 20)
    private RoomStatus oldRoomStatusAfterMove;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folio_item_id")
    private FolioItem folioItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moved_by")
    private User movedBy;

    @Column(name = "moved_at", nullable = false)
    private Instant movedAt;

    @PrePersist
    protected void onCreate() {
        if (this.movedAt == null) {
            this.movedAt = Instant.now();
        }
        if (this.priceDifferencePerNight == null) {
            this.priceDifferencePerNight = BigDecimal.ZERO;
        }
        if (this.chargedNights == null) {
            this.chargedNights = 0;
        }
        if (this.extraChargeAmount == null) {
            this.extraChargeAmount = BigDecimal.ZERO;
        }
    }
}