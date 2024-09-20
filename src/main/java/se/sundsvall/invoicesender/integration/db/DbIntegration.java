package se.sundsvall.invoicesender.integration.db;

import static java.util.Optional.ofNullable;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_IGNORED;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_PROCESSABLE;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_SENT;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.model.Batch;

@Component
public class DbIntegration {

	private final BatchRepository batchRepository;

	DbIntegration(final BatchRepository batchRepository) {
		this.batchRepository = batchRepository;
	}

	public Page<BatchDto> getBatches(final LocalDate from, final LocalDate to, final Pageable pageRequest, final String municipalityId) {
		final var result = batchRepository.findAllByCompletedAtBetweenAndMunicipalityId(
			ofNullable(from).map(LocalDate::atStartOfDay).orElse(null),
			ofNullable(to).map(LocalDate::atStartOfDay).map(t -> t.plusDays(1)).orElse(null),
			municipalityId, pageRequest);

		return new PageImpl<>(result.getContent().stream().map(batchEntity -> mapToBatchDto(batchEntity, false)).toList(), pageRequest, result.getTotalElements());
	}

	@Transactional
	public BatchDto storeBatch(final Batch batch, final String municipalityId) {
		final var items = ofNullable(batch.getItems()).orElse(List.of());

		final var batchEntity = new BatchEntity()
			.withBasename(batch.getBasename())
			.withMunicipalityId(municipalityId)
			.withStartedAt(batch.getStartedAt())
			.withCompletedAt(batch.getCompletedAt())
			.withItems(items.stream()
				.filter(ITEM_IS_AN_INVOICE)
				.map(item -> new ItemEntity()
					.withStatus(item.getStatus())
					.withFilename(item.getFilename()))
				.toList())
			.withTotalItems(items.stream()
				.filter(ITEM_IS_PROCESSABLE)
				.count())
			.withIgnoredItems(items.stream()
				.filter(ITEM_IS_IGNORED)
				.count())
			.withSentItems(items.stream()
				.filter(ITEM_IS_PROCESSABLE)
				.filter(ITEM_IS_SENT)
				.count());

		batchRepository.save(batchEntity);

		return mapToBatchDto(batchEntity, batch.isProcessingEnabled());
	}

	BatchDto mapToBatchDto(final BatchEntity batchEntity, final boolean processingEnabled) {
		if (batchEntity == null) {
			return null;
		}

		return new BatchDto(
			batchEntity.getId(),
			batchEntity.getBasename(),
			batchEntity.getStartedAt(),
			batchEntity.getCompletedAt(),
			batchEntity.getTotalItems(),
			batchEntity.getSentItems(),
			processingEnabled);
	}

}
