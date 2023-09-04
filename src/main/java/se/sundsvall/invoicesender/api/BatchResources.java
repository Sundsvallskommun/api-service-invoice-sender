package se.sundsvall.invoicesender.api;

import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.Problem;

import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Batch Resources")
@RestController
/*@RequestMapping(
    consumes = { APPLICATION_JSON_VALUE },
    produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE }
)*/
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
    @GetMapping
    ResponseEntity<List<BatchEntity>> getAll() {
        var batches = dbIntegration.getAllBatches();

        if (batches.isEmpty()) {
            return noContent().build();
        }

        return ok(batches);
    }

    @Operation(
        summary = "Returns a batch",
        responses = {
            @ApiResponse(
                responseCode = "200", description = "Successful operation", useReturnTypeSchema = true
            ),
            @ApiResponse(
                responseCode = "404", description = "Not found"
            )
        }
    )
    @GetMapping("/{id}")
    ResponseEntity<BatchEntity> getBatch(@PathVariable("id") final Integer id) {
        return ok(dbIntegration.getBatch(id));
    }
}
