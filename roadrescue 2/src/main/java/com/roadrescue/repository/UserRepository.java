package com.roadrescue.repository;

import com.roadrescue.entity.User;
import com.roadrescue.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByPhone(String phone);

    Page<User> findByDeletedFalse(Pageable pageable);

    Page<User> findByRoleAndDeletedFalse(Role role, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.deleted = false AND " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<User> searchUsers(String keyword, Pageable pageable);

    long countByRoleAndDeletedFalse(Role role);
    boolean existsByRole(Role role);
}
