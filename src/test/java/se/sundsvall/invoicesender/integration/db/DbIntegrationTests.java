package se.sundsvall.invoicesender.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.invoicesender.TestDataFactory.createBatchEntity;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;
import static se.sundsvall.invoicesender.integration.db.entity.BatchStatus.MANAGED;
import static se.sundsvall.invoicesender.integration.db.entity.BatchStatus.READY;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;

@ExtendWith(MockitoExtension.class)
class DbIntegrationTests {

	@Mock
	private BatchRepository batchRepositoryMock;

	@Mock
	private ItemRepository itemRepositoryMock;

	@InjectMocks
	private DbIntegration dbIntegration;

	@Test
	void testGetBatches() {
		final var batchEntities = List.of(new BatchEntity(), new BatchEntity(), new BatchEntity());
		when(batchRepositoryMock.findAllByCompletedAtBetweenAndMunicipalityId(
			any(LocalDateTime.class), any(LocalDateTime.class), any(String.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(batchEntities));

		final var result = dbIntegration.getBatches(LocalDate.now(), LocalDate.now(), PageRequest.of(0, 2), "2281");

		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3L);
		assertThat(result.getSize()).isEqualTo(2);
		assertThat(result.getNumber()).isZero();
		assertThat(result.getTotalPages()).isEqualTo(2);

		verify(batchRepositoryMock).findAllByCompletedAtBetweenAndMunicipalityId(
			any(LocalDateTime.class), any(LocalDateTime.class), any(String.class), any(Pageable.class));
		verifyNoMoreInteractions(batchRepositoryMock);
	}

	@Test
	void testGetBatchesByStatusReady() {
		var ready = READY;

		var batchEntity = createBatchEntity();
		var batchEntity2 = createBatchEntity();
		var expectedBatch = List.of(batchEntity, batchEntity2);

		when(batchRepositoryMock.findAllByBatchStatus(ready)).thenReturn(expectedBatch);

		var actualBatch = dbIntegration.getBatchesByStatus(ready);

		assertThat(expectedBatch).isEqualTo(actualBatch);
		assertThat(expectedBatch).hasSize(2);
	}

	@Test
	void testGetBatchesByStatusManaged() {
		var managed = MANAGED;

		var batchEntity = createBatchEntity();
		var batchEntity2 = createBatchEntity();
		var expectedBatch = List.of(batchEntity, batchEntity2);

		when(batchRepositoryMock.findAllByBatchStatus(managed)).thenReturn(expectedBatch);

		var actualBatch = dbIntegration.getBatchesByStatus(managed);

		assertThat(expectedBatch).isEqualTo(actualBatch);
		assertThat(expectedBatch).hasSize(2);
	}

	@Test
	void persistItem() {
		var itemEntity = createItemEntity();

		dbIntegration.persistItem(itemEntity);

		verify(itemRepositoryMock).save(itemEntity);
		verifyNoMoreInteractions(itemRepositoryMock);
		verifyNoInteractions(batchRepositoryMock);
	}

	@Test
	void persistBatch() {
		var batchEntity = createBatchEntity();

		dbIntegration.persistBatch(batchEntity);

		verify(batchRepositoryMock).save(batchEntity);
		verifyNoMoreInteractions(batchRepositoryMock);
		verifyNoInteractions(itemRepositoryMock);
	}

	@Test
	void persistsBatches() {
		var batchEntities = List.of(createBatchEntity(), createBatchEntity());

		dbIntegration.persistBatches(batchEntities);

		verify(batchRepositoryMock).saveAll(batchEntities);
		verifyNoMoreInteractions(batchRepositoryMock);
		verifyNoInteractions(itemRepositoryMock);
	}

}
