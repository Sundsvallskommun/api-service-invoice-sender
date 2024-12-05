package se.sundsvall.invoicesender.api.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

public record BatchDto(

	Integer id,
	String basename,
	@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startedAt,
	@JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime completedAt,
	long totalItems,
	long sentItems,
	@JsonIgnore boolean processingEnabled) {
}
