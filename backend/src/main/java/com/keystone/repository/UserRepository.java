package com.keystone.repository;

import com.keystone.entity.User;
import com.keystone.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserEmail(String userEmail);

    boolean existsByUserEmail(String userEmail);

    List<User> findByRoleAndEnabledOrderByFirstNameAscLastNameAsc(Role role, boolean enabled);

    List<User> findByRoleAndEnabledAndCustomer_Id(Role role, boolean enabled, Long customerId);

    Optional<User> findByRoleAndEnabledAndUserEmail(Role role, boolean enabled, String userEmail);

    long countByRoleAndEnabled(Role role, boolean enabled);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.phone) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:enabled IS NULL OR u.enabled = :enabled)")
    Page<User> searchUsers(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
