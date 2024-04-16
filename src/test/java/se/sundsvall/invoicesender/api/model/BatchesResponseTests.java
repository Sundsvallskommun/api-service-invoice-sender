package se.sundsvall.invoicesender.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import se.sundsvall.invoicesender.integration.db.dto.BatchDto;

class BatchesResponseTests {

    @Test
    void testCreation() {
        var now = LocalDateTime.now();

        var batchesResponse = new BatchesResponse(
            List.of(
                new BatchDto(123, "something", now.minusMinutes(2), now.minusMinutes(1), 5, 3, false),
                new BatchDto(456, "something-else", now.minusMinutes(4), now.minusMinutes(3), 2, 1, false)
            ),
            new BatchesResponse.PaginationInfo(1, 20, 1, 2));

        assertThat(batchesResponse.batches()).isNotNull().hasSize(2).satisfies(batches -> {
            assertThat(batches).extracting(BatchDto::id).containsExactlyInAnyOrder(123, 456);
            assertThat(batches).extracting(BatchDto::startedAt)
                .containsExactlyInAnyOrder(now.minusMinutes(2), now.minusMinutes(4));
            assertThat(batches).extracting(BatchDto::completedAt)
                .containsExactlyInAnyOrder(now.minusMinutes(1), now.minusMinutes(3));
            assertThat(batches).extracting(BatchDto::totalItems).containsExactlyInAnyOrder(5L, 2L);
            assertThat(batches).extracting(BatchDto::sentItems).containsExactlyInAnyOrder(3L, 1L);
        });

        assertThat(batchesResponse.paginationInfo()).isNotNull().satisfies(paginationInfo -> {
            assertThat(paginationInfo.page()).isOne();
            assertThat(paginationInfo.pageSize()).isEqualTo(20);
            assertThat(paginationInfo.totalPages()).isOne();
            assertThat(paginationInfo.totalElements()).isEqualTo(2);
        });
    }
}
