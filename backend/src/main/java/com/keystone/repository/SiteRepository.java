package com.keystone.repository;

import com.keystone.entity.Site;
import com.keystone.enums.SiteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    boolean existsBySiteCode(String siteCode);

    boolean existsBySiteCodeAndIdNot(String siteCode, Long id);

    boolean existsByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"customer"})
    @Query("SELECT s FROM Site s WHERE s.id = :id")
    Optional<Site> findByIdWithCustomer(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer"})
    @Query("SELECT s FROM Site s WHERE " +
           "(:search IS NULL OR LOWER(s.siteCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.siteName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.contactName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.contactEmail) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:customerId IS NULL OR s.customer.id = :customerId)")
    Page<Site> searchSites(
            @Param("search") String search,
            @Param("status") SiteStatus status,
            @Param("customerId") Long customerId,
            Pageable pageable
    );
}
