package se.sundsvall.invoicesender.integration.db;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

@CircuitBreaker(name = "ItemRepository")
@Repository
public interface ItemRepository extends JpaRepository<ItemEntity, Integer> {
}
