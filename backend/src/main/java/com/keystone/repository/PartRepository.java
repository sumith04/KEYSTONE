package com.keystone.repository;

import com.keystone.entity.Part;
import com.keystone.enums.PartStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartRepository extends JpaRepository<Part, Long> {

    Optional<Part> findByPartNumber(String partNumber);

    boolean existsByPartNumber(String partNumber);

    boolean existsByPartNumberAndIdNot(String partNumber, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Part p WHERE p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Part p WHERE " +
           "(:search IS NULL OR LOWER(p.partNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Part> searchParts(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") PartStatus status,
            Pageable pageable
    );
}
