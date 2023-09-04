package se.sundsvall.invoicesender.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;

interface BatchRepository extends JpaRepository<BatchEntity, Integer> {

}
