package se.sundsvall.invoicesender.integration.db.dto;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

public record BatchDto(

	Integer id,
	String basename,
	@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
	@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completedAt,
	long totalItems,
	long sentItems,
	@JsonIgnore boolean processingEnabled) {}
