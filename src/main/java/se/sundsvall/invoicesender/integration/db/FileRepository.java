package se.sundsvall.invoicesender.integration.db;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.invoicesender.integration.db.entity.FileEntity;

public interface FileRepository extends JpaRepository<FileEntity, Integer> {

}
