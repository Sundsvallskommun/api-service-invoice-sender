package se.sundsvall.invoicesender.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import se.sundsvall.invoicesender.integration.db.dto.BatchDto;

@JsonPropertyOrder({
	"batches", "pagination"
})
public record BatchesResponse(

	List<BatchDto> batches,
	@JsonProperty("pagination") PaginationInfo paginationInfo) {

	public record PaginationInfo(
		int page,
		int pageSize,
		int totalPages,
		long totalElements) {}
}
