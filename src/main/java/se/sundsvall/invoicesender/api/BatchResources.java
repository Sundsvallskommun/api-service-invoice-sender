package se.sundsvall.invoicesender.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

import java.time.LocalDate;

import jakarta.validation.constraints.Positive;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;

import se.sundsvall.invoicesender.api.model.BatchesResponse;
import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Batch Resources")
@RestController
@Validated
@RequestMapping("/batches")
@ApiResponse(
    responseCode = "500",
    description = "Internal server error",
    content = @Content(schema = @Schema(implementation = Problem.class))
)
class BatchResources {

    private final DbIntegration dbIntegration;

    BatchResources(final DbIntegration dbIntegration) {
        this.dbIntegration = dbIntegration;
    }

    @Operation(
        summary = "Returns all batches",
        responses = {
            @ApiResponse(
                responseCode = "200", description = "Successful operation", useReturnTypeSchema = true
            ),
            @ApiResponse(
                responseCode = "204", description = "No content"
            )
        }
    )
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    ResponseEntity<BatchesResponse> getAll(
            @Parameter(description = "Completed from-date (inclusive). Format: yyyy-MM-dd")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false)
            final LocalDate from,

            @Parameter(description = "Completed to-date (inclusive). Format: yyyy-MM-dd")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false)
            final LocalDate to,

            @Parameter(description = "Page (1-based)")
            @Positive
            @RequestParam(defaultValue = "1")
            final int page,

            @Parameter(description = "Page size (default: 20)")
            @Positive
            @RequestParam(defaultValue = "20")
            final int pageSize) {
        var batches = dbIntegration.getAllBatches(from, to, PageRequest.of(page - 1, pageSize, Sort.by("completedAt").descending()));

        if (batches.isEmpty()) {
            return noContent().build();
        }

        return ok(mapToResponse(batches));
    }

    BatchesResponse mapToResponse(final Page<BatchEntity> batchPage) {
        return new BatchesResponse(
            batchPage.stream()
                .map(this::mapBatch)
                .toList(),
            mapPaginationInfo(batchPage));
    }

    BatchesResponse.Batch mapBatch(final BatchEntity batch) {
        return new BatchesResponse.Batch(
            batch.getId(),
            batch.getStartedAt(),
            batch.getCompletedAt(),
            batch.getTotalItems(),
            batch.getSentItems());
    }

    BatchesResponse.PaginationInfo mapPaginationInfo(final Page<?> batchPage) {
        return new BatchesResponse.PaginationInfo(
            batchPage.getNumber() + 1,
            batchPage.getSize(),
            batchPage.getTotalPages(),
            batchPage.getTotalElements());
    }
}
