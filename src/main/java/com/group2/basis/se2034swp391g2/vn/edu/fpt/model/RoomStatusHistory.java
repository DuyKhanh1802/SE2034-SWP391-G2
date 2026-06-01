package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "room_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RoomStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_rsh_room"))
    private Room room;

    @Column(name = "old_status", nullable = false, length = 15)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 15)
    private String newStatus;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt = Instant.now();
}