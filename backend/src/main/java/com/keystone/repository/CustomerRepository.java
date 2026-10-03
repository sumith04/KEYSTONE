package com.keystone.repository;

import com.keystone.entity.Customer;
import com.keystone.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCustomerCode(String customerCode);

    boolean existsByCustomerCodeAndIdNot(String customerCode, Long id);

    boolean existsByEmail(String email);

    Optional<Customer> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsBySlaPolicyId(Long slaPolicyId);

    @EntityGraph(attributePaths = {"slaPolicy"})
    @Query("SELECT c FROM Customer c WHERE c.id = :id")
    Optional<Customer> findByIdWithSlaPolicy(@Param("id") Long id);

    @EntityGraph(attributePaths = {"slaPolicy"})
    @Query("SELECT c FROM Customer c WHERE " +
           "(:search IS NULL OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Customer> searchCustomers(
            @Param("search") String search,
            @Param("status") CustomerStatus status,
            Pageable pageable
    );
}
