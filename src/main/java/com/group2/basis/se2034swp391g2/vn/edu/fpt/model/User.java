package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.ApprovalStatus;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.DietaryPref;
import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.UserType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "user_type", nullable = false, length = 10)
    private UserType userType = UserType.GUEST;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.APPROVED;

    @Column(name = "approval_note", length = 300)
    private String approvalNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", foreignKey = @ForeignKey(name = "fk_users_reviewed_by"))
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "first_name", nullable = false, length = 50)
    @Nationalized
    private String firstName;

    @Nationalized
    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "identity_card", length = 20)
    private String identityCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nationality_id", foreignKey = @ForeignKey(name = "fk_users_nationality"))
    private Country nationality;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_room_type_id", foreignKey = @ForeignKey(name = "fk_users_preferred_type"))
    private RoomType preferredRoomType;

    @Builder.Default
    @Column(name = "total_stays", nullable = false)
    private Integer totalStays = 0;

    @Builder.Default
    @Column(name = "total_spent", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(name = "last_stay_at")
    private Instant lastStayAt;

    @Column(name = "internal_notes", columnDefinition = "NVARCHAR(MAX)")
    @Nationalized
    private String internalNotes;

    @Column(name = "health_tags", length = 500)
    private String healthTags;

    @Column(name = "pregnancy_month")
    private Byte pregnancyMonth;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "dietary_pref", nullable = false, length = 20)
    private DietaryPref dietaryPref = DietaryPref.NORMAL;

    @Column(name = "spa_preferences", length = 300)
    @Nationalized
    private String spaPreferences;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns        = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"),
            foreignKey         = @ForeignKey(name = "fk_ur_user"),
            inverseForeignKey  = @ForeignKey(name = "fk_ur_role")
    )
    private Set<Role> roles = new HashSet<>();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}