package se.sundsvall.invoicesender.integration.db;

import static java.util.Optional.ofNullable;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import se.sundsvall.invoicesender.api.model.BatchDto;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;

@Component
@Transactional
public class DbIntegration {

	private final BatchRepository batchRepository;
	private final ItemRepository itemRepository;

	DbIntegration(final BatchRepository batchRepository, ItemRepository itemRepository) {
		this.batchRepository = batchRepository;
		this.itemRepository = itemRepository;
	}

	public Page<BatchDto> getBatches(final LocalDate from, final LocalDate to, final Pageable pageRequest, final String municipalityId) {
		final var result = batchRepository.findAllByCompletedAtBetweenAndMunicipalityId(
			ofNullable(from).map(LocalDate::atStartOfDay).orElse(null),
			ofNullable(to).map(LocalDate::atStartOfDay).map(t -> t.plusDays(1)).orElse(null),
			municipalityId, pageRequest);

		return new PageImpl<>(result.getContent().stream()
			.map(this::mapToBatchDto)
			.toList(), pageRequest, result.getTotalElements());
	}

	public void persistItem(final ItemEntity itemEntity) {
		itemRepository.save(itemEntity);
	}

	public void persistBatch(final BatchEntity batchEntity) {
		batchRepository.save(batchEntity);
	}

	public List<BatchEntity> persistBatches(final List<BatchEntity> batches) {
		return batchRepository.saveAll(batches);
	}

	BatchDto mapToBatchDto(final BatchEntity batchEntity) {
		return Optional.ofNullable(batchEntity).map(batch -> new BatchDto(
			batch.getId(),
			batch.getBasename(),
			batch.getStartedAt(),
			batch.getCompletedAt(),
			batch.getTotalItems(),
			batch.getSentItems(),
			false))
			.orElse(null);
	}

}
