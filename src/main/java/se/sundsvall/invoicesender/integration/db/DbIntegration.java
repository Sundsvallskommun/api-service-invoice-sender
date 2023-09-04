package se.sundsvall.invoicesender.integration.db;

import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.util.List;

import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.model.Batch;

@Component
public class DbIntegration {

    private final BatchRepository batchRepository;

    public DbIntegration(final BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public List<BatchEntity> getAllBatches() {
        return batchRepository.findAll();
    }

    public BatchEntity getBatch(final Integer id) {
        return batchRepository.findById(id)
            .orElseThrow(() -> Problem.valueOf(Status.NOT_FOUND));
    }

    public BatchEntity storeBatchExecution(final Batch batch) {
        var batchEntity = new BatchEntity()
            .setStartedAt(batch.getStartedAt())
            .setCompletedAt(batch.getCompletedAt())
            .setTotalItems(batch.getItems().stream()
                .filter(item -> item.getStatus() != NOT_AN_INVOICE)
                .count())
            .setSentItems(batch.getItems().stream()
                .filter(item -> item.getStatus() == SENT)
                .count());

        return batchRepository.save(batchEntity);
    }
}
