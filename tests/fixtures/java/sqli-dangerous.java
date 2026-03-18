package com.example.repo;

import java.sql.*;
import javax.persistence.EntityManager;

/**
 * UserRepository — intentionally vulnerable SQL examples.
 * Each method is annotated with the expected security finding.
 *
 * DO NOT USE IN PRODUCTION. This file exists solely as a test fixture
 * for the /appsec:check skill validation suite.
 */
public class UserRepository {

    private Connection connection;
    private EntityManager entityManager;

    /**
     * Finds a user by ID using direct string concatenation.
     * EXPECT: HIGH - SQL injection via userId parameter
     */
    public ResultSet findById(String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = " + userId;
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    /**
     * Finds a user by name using createStatement with concatenated SQL.
     * EXPECT: HIGH - SQL injection via name parameter
     */
    public ResultSet findByName(String name) throws SQLException {
        String sql = "SELECT * FROM users WHERE name = '" + name + "'";
        return connection.createStatement().execute(sql)
            ? connection.createStatement().executeQuery(sql)
            : null;
    }

    /**
     * Fetches orders by status using JPQL string concatenation.
     * EXPECT: HIGH - SQL injection via status parameter in JPQL query
     */
    public Object findOrdersByStatus(String status) {
        String jpql = "SELECT o FROM Order o WHERE o.status = " + status;
        return entityManager.createQuery(jpql).getResultList();
    }

    /**
     * Finds a user by email using String.format with Statement.executeQuery.
     * EXPECT: HIGH - SQL injection via email parameter
     */
    public ResultSet findByEmail(String email) throws SQLException {
        String sql = String.format("SELECT * FROM users WHERE email = '%s'", email);
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    /**
     * Deletes a session using PreparedStatement but with concatenated SQL —
     * the PreparedStatement name is misleading; the SQL itself is built by concatenation.
     * EXPECT: HIGH - SQL injection via token parameter despite PreparedStatement usage
     */
    public void deleteSession(String token) throws SQLException {
        String sql = "DELETE FROM sessions WHERE token = '" + token + "'";
        // BUG: PreparedStatement created with concatenated SQL, NOT with ? placeholders.
        // This is still vulnerable — the parameterization never happens.
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.execute();
    }

    /**
     * Searches users by role using Statement.execute with concatenated SQL.
     * EXPECT: HIGH - SQL injection via role parameter
     */
    public ResultSet findByRole(String role) throws SQLException {
        String sql = "SELECT * FROM users WHERE role = '" + role + "' AND active = 1";
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }
}
