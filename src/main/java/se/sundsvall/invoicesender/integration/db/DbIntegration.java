package se.sundsvall.invoicesender.integration.db;

import static java.util.Optional.ofNullable;
import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Status.SENT;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.FileEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.model.Batch;

@Component
public class DbIntegration {

    private final BatchRepository batchRepository;
    private final FileRepository fileRepository;

    DbIntegration(final BatchRepository batchRepository, final FileRepository fileRepository) {
        this.batchRepository = batchRepository;
        this.fileRepository = fileRepository;
    }

    public Page<BatchDto> getBatches(final LocalDate from, final LocalDate to, final Pageable pageRequest) {
        var result = batchRepository.findAllByCompletedAtBetween(
            ofNullable(from).map(LocalDate::atStartOfDay).orElse(null),
            ofNullable(to).map(LocalDate::atStartOfDay).map(t -> t.plusDays(1)).orElse(null),
            pageRequest);

        return new PageImpl<>(result.getContent().stream().map(this::mapToBatchDto).toList(), pageRequest, result.getTotalElements());
    }

    @Transactional
    public BatchDto storeBatch(final Batch batch) {
        var items = ofNullable(batch.getItems()).orElse(List.of());

        var batchEntity = new BatchEntity()
            .withBasename(batch.getBasename())
            .withStartedAt(batch.getStartedAt())
            .withCompletedAt(batch.getCompletedAt())
            .withItems(items.stream()
                .filter(item -> item.getStatus() != NOT_AN_INVOICE)
                .map(item -> new ItemEntity()
                    .withFilename(item.getFilename())
                    .withStatus(item.getStatus()))
                .toList())
            .withTotalItems(items.stream()
                .filter(item -> item.getStatus() != NOT_AN_INVOICE)
                .count())
            .withSentItems(items.stream()
                .filter(item -> item.getStatus() == SENT)
                .count());

        batchRepository.save(batchEntity);
        fileRepository.save(new FileEntity()
            .withBatch(batchEntity)
            .withFilename(batch.getBasename() + ".zip.7z")
            .withData(batch.getData()));

        return mapToBatchDto(batchEntity);
    }

    BatchDto mapToBatchDto(final BatchEntity batchEntity) {
        if (batchEntity == null) {
            return null;
        }

        return new BatchDto(
            batchEntity.getId(),
            batchEntity.getBasename(),
            batchEntity.getStartedAt(),
            batchEntity.getCompletedAt(),
            batchEntity.getTotalItems(),
            batchEntity.getSentItems());
    }
}
