package se.sundsvall.invoicesender.integration.db;

import static java.util.Optional.ofNullable;
import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.model.Batch;

@Component
public class DbIntegration {

    private final BatchRepository batchRepository;

    DbIntegration(final BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public Page<BatchEntity> getAllBatches(final LocalDate from, final LocalDate to, final Pageable pageRequest) {
        return batchRepository.findAllByCompletedAtBetween(
            ofNullable(from).map(LocalDate::atStartOfDay).orElse(null),
            ofNullable(to).map(LocalDate::atStartOfDay).map(t -> t.plusDays(1)).orElse(null),
            pageRequest);
    }

    public BatchEntity storeBatchExecution(final Batch batch) {
        var batchEntity = new BatchEntity()
            .withStartedAt(batch.getStartedAt())
            .withCompletedAt(batch.getCompletedAt())
            .withItems(batch.getItems().stream()
                .filter(item -> item.getStatus() != NOT_AN_INVOICE)
                .map(item -> new ItemEntity()
                    .setFilename(item.getFilename())
                    .setStatus(item.getStatus()))
                .toList())
            .withTotalItems(batch.getItems().stream()
                .filter(item -> item.getStatus() != NOT_AN_INVOICE)
                .count())
            .withSentItems(batch.getItems().stream()
                .filter(item -> item.getStatus() == SENT)
                .count());

        return batchRepository.save(batchEntity);
    }
}
