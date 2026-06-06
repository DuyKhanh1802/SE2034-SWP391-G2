package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.Gender;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.IdentityType;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false, length = 10,columnDefinition = "NVARCHAR(10)")
    private UserType userType = UserType.GUEST;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20,columnDefinition = "NVARCHAR(20)")
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @Column(name = "approval_note", length = 300,columnDefinition = "NVARCHAR(300)")
    private String approvalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;


    @Column(name = "first_name", nullable = false, length = 50,columnDefinition = "NVARCHAR(50)")
    private String firstName;


    @Column(name = "last_name", nullable = false, length = 50,columnDefinition = "NVARCHAR(50)")
    private String lastName;

    @Column(name = "email", length = 150,columnDefinition = "NVARCHAR(150)")
    private String email;

    @Column(name = "phone", length = 20,columnDefinition = "NVARCHAR(20)")
    private String phone;

    @Column(name = "password_hash", length = 255,columnDefinition = "NVARCHAR(255)")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", length = 20,columnDefinition = "NVARCHAR(20)")
    private IdentityType identityType;

    @Column(name = "identity_number", length = 50,columnDefinition = "NVARCHAR(50)")
    private String identityNumber;

    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    @Column(name = "date_of_birth")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10, columnDefinition = "NVARCHAR(10)")
    private Gender gender;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_type_id")
    private RoomType preferredRoomType;

    @Column(name = "total_stays", nullable = false)
    private Integer totalStays = 0;

    @Column(name = "total_spent", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "last_stay_at")
    private Instant lastStayAt;

    @Column(name = "internal_notes",columnDefinition = "NVARCHAR(MAX)")
    private String internalNotes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    @OneToMany(mappedBy = "user",fetch = FetchType.EAGER,cascade = CascadeType.ALL)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();
}