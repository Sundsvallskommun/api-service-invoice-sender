package se.sundsvall.invoicesender.integration.db;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;

interface BatchRepository extends JpaRepository<BatchEntity, Integer> {

    @Query("""
        SELECT b FROM BatchEntity b WHERE
        (:from IS NULL OR b.completedAt >= :from) AND
        (:to IS NULL OR b.completedAt <= :to)
    """)
    Page<BatchEntity> findAllByCompletedAtBetween(
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageRequest);
}
