package se.sundsvall.invoicesender.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.BatchStatus;

@CircuitBreaker(name = "BatchRepository")
interface BatchRepository extends JpaRepository<BatchEntity, Integer> {

	@Query("""
			SELECT b FROM BatchEntity b WHERE
			(:from IS NULL OR b.completedAt >= :from) AND
			(:to IS NULL OR b.completedAt <= :to) AND
			(:municipalityId IS NULL OR b.municipalityId = :municipalityId)
		""")
	Page<BatchEntity> findAllByCompletedAtBetweenAndMunicipalityId(
		@Param("from") LocalDateTime from,
		@Param("to") LocalDateTime to,
		@Param("municipalityId") String municipalityId,
		Pageable pageRequest);

	List<BatchEntity> findAllByBatchStatus(BatchStatus batchStatus);

}
