package com.group2.basis.se2034swp391g2.vn.edu.fpt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long id;

    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "permission_name", nullable = false, length = 150, columnDefinition = "NVARCHAR(150)")
    private String name;

    @Column(name = "description", length = 300, columnDefinition = "NVARCHAR(300)")
    private String description;

    @OneToMany(mappedBy = "permission")
    @Builder.Default
    private Set<RolePermission> rolePermissions = new HashSet<>();
}
