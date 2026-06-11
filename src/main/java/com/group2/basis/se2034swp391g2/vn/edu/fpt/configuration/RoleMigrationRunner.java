package com.group2.basis.se2034swp391g2.vn.edu.fpt.configuration;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
@RequiredArgsConstructor
public class RoleMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleMigrationRunner.class);

    private final DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String roleConstraintName = findRoleNameConstraintName(connection);
                if (roleConstraintName != null) {
                    replaceRoleNameConstraint(connection, roleConstraintName,
                            "SYSTEM_ADMIN", "HOTEL_ADMIN", "ADMIN", "MANAGER", "RECEPTIONIST", "GUEST");
                }

                Long systemAdminRoleId = ensureRole(connection, "SYSTEM_ADMIN");
                ensureRole(connection, "HOTEL_ADMIN");
                ensureRole(connection, "MANAGER");
                ensureRole(connection, "RECEPTIONIST");
                ensureRole(connection, "GUEST");

                Long legacyAdminRoleId = findRoleId(connection, "ADMIN");
                if (legacyAdminRoleId != null) {
                    migrateLegacyAdminUsers(connection, legacyAdminRoleId, systemAdminRoleId);
                    deleteRoleIfUnused(connection, legacyAdminRoleId);
                    log.info("Migrated legacy ADMIN role to SYSTEM_ADMIN.");
                }

                if (roleConstraintName != null) {
                    replaceRoleNameConstraint(connection, roleConstraintName,
                            "SYSTEM_ADMIN", "HOTEL_ADMIN", "MANAGER", "RECEPTIONIST", "GUEST");
                }

                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private Long ensureRole(Connection connection, String roleName) throws Exception {
        Long roleId = findRoleId(connection, roleName);
        if (roleId != null) {
            return roleId;
        }

        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO roles (role_name) VALUES (?)")) {
            insert.setString(1, roleName);
            insert.executeUpdate();
        }

        Long insertedRoleId = findRoleId(connection, roleName);
        if (insertedRoleId == null) {
            throw new IllegalStateException("Failed to create missing role: " + roleName);
        }
        return insertedRoleId;
    }

    private Long findRoleId(Connection connection, String roleName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT role_id FROM roles WHERE role_name = ?")) {
            statement.setString(1, roleName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
            }
        }
        return null;
    }

    private void migrateLegacyAdminUsers(Connection connection,
                                         Long legacyAdminRoleId,
                                         Long systemAdminRoleId) throws Exception {
        try (PreparedStatement update = connection.prepareStatement(
                """
                UPDATE ur
                SET role_id = ?
                FROM user_roles ur
                WHERE ur.role_id = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM user_roles ur2
                      WHERE ur2.user_id = ur.user_id
                        AND ur2.role_id = ?
                  )
                """)) {
            update.setLong(1, systemAdminRoleId);
            update.setLong(2, legacyAdminRoleId);
            update.setLong(3, systemAdminRoleId);
            update.executeUpdate();
        }

        try (PreparedStatement delete = connection.prepareStatement(
                "DELETE FROM user_roles WHERE role_id = ?")) {
            delete.setLong(1, legacyAdminRoleId);
            delete.executeUpdate();
        }
    }

    private String findRoleNameConstraintName(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                SELECT name
                FROM sys.check_constraints
                WHERE parent_object_id = OBJECT_ID('roles')
                  AND definition LIKE '%[role_name]%'
                """)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        return null;
    }

    private void replaceRoleNameConstraint(Connection connection,
                                           String constraintName,
                                           String... allowedRoles) throws Exception {
        try (PreparedStatement drop = connection.prepareStatement(
                "ALTER TABLE roles DROP CONSTRAINT [" + constraintName + "]")) {
            drop.executeUpdate();
        }

        StringBuilder constraintSql = new StringBuilder(
                "ALTER TABLE roles ADD CONSTRAINT [" + constraintName + "] CHECK ([role_name] IN (");
        for (int i = 0; i < allowedRoles.length; i++) {
            if (i > 0) {
                constraintSql.append(", ");
            }
            constraintSql.append("'").append(allowedRoles[i]).append("'");
        }
        constraintSql.append("))");

        try (PreparedStatement add = connection.prepareStatement(constraintSql.toString())) {
            add.executeUpdate();
        }
    }

    private void deleteRoleIfUnused(Connection connection, Long roleId) throws Exception {
        try (PreparedStatement count = connection.prepareStatement(
                "SELECT COUNT(*) FROM user_roles WHERE role_id = ?")) {
            count.setLong(1, roleId);
            try (ResultSet resultSet = count.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) == 0) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM roles WHERE role_id = ?")) {
                        delete.setLong(1, roleId);
                        delete.executeUpdate();
                    }
                }
            }
        }
    }
}
