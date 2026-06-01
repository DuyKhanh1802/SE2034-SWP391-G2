package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Department;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Entity
@Table(name = "work_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_order_id")
    private Long workOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_wo_booking"))
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_wo_service"))
    private Service service;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "guest_notes", length = 500)
    @Nationalized
    private String guestNotes;

    @Column(name = "staff_notes", length = 500)
    @Nationalized
    private String staffNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private WorkOrderStatus status = WorkOrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "department", nullable = false, length = 15)
    private Department department;

    /** 1 = high, 2 = normal, 3 = low */
    @Column(name = "priority", nullable = false)
    private Byte priority = 2;

    @Column(name = "health_alert", length = 500)
    @Nationalized
    private String healthAlert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to",
            foreignKey = @ForeignKey(name = "fk_wo_assigned_to"))
    private User assignedTo;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}