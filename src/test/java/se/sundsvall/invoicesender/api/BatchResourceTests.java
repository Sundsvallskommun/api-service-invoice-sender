package se.sundsvall.invoicesender.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zalando.problem.Status;
import org.zalando.problem.violations.ConstraintViolationProblem;
import org.zalando.problem.violations.Violation;

import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.api.model.BatchesResponse;
import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.service.InvoiceProcessor;

@ActiveProfiles("junit")
@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
class BatchResourceTests {

    @MockBean
    private DbIntegration mockDbIntegration;
    @MockBean
    private InvoiceProcessor mockInvoiceProcessor;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void triggerBatchWithInvalidData() {
        var response = webTestClient.post()
            .uri("/batches/trigger/not-a-date")
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ConstraintViolationProblem.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);

        verifyNoInteractions(mockDbIntegration);
    }

    @Test
    void triggerBatch() throws Exception {
        var date = LocalDate.of(2019, 2, 28);

        webTestClient.post()
            .uri("/batches/trigger/{date}", date.format(DateTimeFormatter.ISO_DATE))
            .exchange()
            .expectStatus().isOk();


        verify(mockInvoiceProcessor, times(1)).run(eq(date));
        verifyNoMoreInteractions(mockInvoiceProcessor);
        verifyNoInteractions(mockDbIntegration);
    }

    @Test
    void getAllWithInvalidPagingData() {
        var response = webTestClient.get()
            .uri("/batches?page={page}&pageSize={pageSize}", 0, 0)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(ConstraintViolationProblem.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(Status.BAD_REQUEST);
        assertThat(response.getViolations()).hasSize(2).extracting(Violation::getField)
            .containsExactlyInAnyOrder("getAll.page", "getAll.pageSize");

        verifyNoInteractions(mockDbIntegration);
    }

    @Test
    void getAllWhenNothingIsFound() {
        when(mockDbIntegration.getBatches(nullable(LocalDate.class), nullable(LocalDate.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of()));

        webTestClient.get()
            .uri("/batches")
            .exchange()
            .expectStatus().isNoContent();

        verify(mockDbIntegration, times(1)).getBatches(nullable(LocalDate.class), nullable(LocalDate.class), any(PageRequest.class));
        verifyNoMoreInteractions(mockDbIntegration);
    }

    @Test
    void getAll() {
        when(mockDbIntegration.getBatches(nullable(LocalDate.class), nullable(LocalDate.class), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(
                new BatchDto(1, "something", LocalDateTime.now(), LocalDateTime.now(), 1, 2),
                new BatchDto(2, "something-else", LocalDateTime.now(), LocalDateTime.now(), 3, 4))));

        var response = webTestClient.get()
            .uri("/batches")
            .exchange()
            .expectStatus().isOk()
            .expectBody(BatchesResponse.class)
            .returnResult()
            .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.batches()).isNotNull().hasSize(2);

        verify(mockDbIntegration, times(1)).getBatches(nullable(LocalDate.class), nullable(LocalDate.class), any(PageRequest.class));
        verifyNoMoreInteractions(mockDbIntegration);
    }
}
