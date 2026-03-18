package com.example.repo;

import java.sql.*;
import java.util.List;
import javax.persistence.*;
import javax.persistence.criteria.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * SafeUserRepository — parameterized query examples.
 * Each method is annotated with the expected security outcome.
 *
 * This file exists as a test fixture for the /appsec:check skill validation
 * suite. The skill should produce zero findings against this file.
 */
public class SafeUserRepository {

    private Connection connection;
    private EntityManager entityManager;

    /**
     * Finds a user by ID using PreparedStatement with ? placeholder.
     * EXPECT: NO FINDING
     */
    public ResultSet findById(String userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, userId);
        return ps.executeQuery();
    }

    /**
     * Finds a user by email and status using multiple ? placeholders.
     * EXPECT: NO FINDING
     */
    public ResultSet findByEmailAndStatus(String email, String status) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ? AND status = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, email);
        ps.setString(2, status);
        return ps.executeQuery();
    }

    /**
     * Spring Data JPA named parameter query — safe by design.
     * EXPECT: NO FINDING
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status")
    public List<Object> findActiveByEmail(@Param("email") String email,
                                          @Param("status") String status) {
        // Spring Data handles parameterization — no direct query construction
        return entityManager
            .createNamedQuery("User.findByEmailAndStatus")
            .setParameter("email", email)
            .setParameter("status", status)
            .getResultList();
    }

    /**
     * Spring Data derived query method — safe, no SQL written at all.
     * EXPECT: NO FINDING
     */
    public interface UserRepository {
        List<Object> findByEmailAndStatus(String email, String status);
        List<Object> findByRoleOrderByCreatedAtDesc(String role);
    }

    /**
     * JPA Criteria API — parameterized by construction.
     * EXPECT: NO FINDING
     */
    public List<Object> findByEmailCriteria(String email) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> cq = cb.createQuery(Object.class);
        Root<Object> root = cq.from(Object.class);
        cq.where(cb.equal(root.get("email"), email));
        return entityManager.createQuery(cq).getResultList();
    }

    /**
     * Hardcoded SQL constant with no user input — no taint path possible.
     * EXPECT: NO FINDING
     */
    public ResultSet countAllUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    /**
     * PreparedStatement with setLong for numeric ID — properly parameterized.
     * EXPECT: NO FINDING
     */
    public ResultSet findOrdersByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, userId);
        return ps.executeQuery();
    }
}
