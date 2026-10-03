package com.keystone.repository;

import com.keystone.entity.SlaPolicy;
import com.keystone.enums.WorkOrderPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("SELECT p FROM SlaPolicy p WHERE " +
           "(:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:priority IS NULL OR p.priority = :priority) AND " +
           "(:active IS NULL OR p.active = :active)")
    Page<SlaPolicy> searchPolicies(
            @Param("search") String search,
            @Param("priority") WorkOrderPriority priority,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
