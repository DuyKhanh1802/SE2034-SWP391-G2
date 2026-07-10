package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;


import com.group2.basis.se2034swp391g2.vn.edu.fpt.common.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

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
}
