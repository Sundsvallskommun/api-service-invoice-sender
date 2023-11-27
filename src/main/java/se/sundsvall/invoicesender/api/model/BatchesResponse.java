package se.sundsvall.invoicesender.api.model;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"batches", "pagination"})
public record BatchesResponse(

    List<Batch> batches,
    @JsonProperty("pagination")
    PaginationInfo paginationInfo) {

    public record Batch(

        Integer id,
        @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startedAt,
        @JsonFormat(shape = STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime completedAt,
        long totalItems,
        long sentItems) { }

    public record PaginationInfo(
        int page,
        int pageSize,
        int totalPages,
        long totalElements) { }
}
