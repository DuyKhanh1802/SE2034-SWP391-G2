package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;
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
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_name", nullable = false, unique = true, length = 20)
    private RoleName roleName;

    @OneToMany(mappedBy = "role", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();
}
