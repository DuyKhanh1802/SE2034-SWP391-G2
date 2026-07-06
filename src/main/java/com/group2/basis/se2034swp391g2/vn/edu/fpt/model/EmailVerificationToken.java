package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_verification_tokens")
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long id;

    // Field token sẽ lưu OTP 6 số
    @Column(name = "token", nullable = false, length = 20, columnDefinition = "NVARCHAR(20)")
    private String token;

    @Column(name = "expiry_time", nullable = false)
    private Instant expiryTime;

    @Column(name = "is_used", nullable = false)
    private Boolean isUsed = false;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }

        if (this.isUsed == null) {
            this.isUsed = false;
        }

        if (this.attemptCount == null) {
            this.attemptCount = 0;
        }
    }
}