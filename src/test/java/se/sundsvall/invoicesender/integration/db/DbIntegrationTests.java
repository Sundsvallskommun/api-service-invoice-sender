package se.sundsvall.invoicesender.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.FileEntity;
import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;
import se.sundsvall.invoicesender.model.Status;

@ExtendWith(MockitoExtension.class)
class DbIntegrationTests {

    @Mock
    private BatchRepository mockBatchRepository;
    @Mock
    private FileRepository mockFileRepository;
    @InjectMocks
    private DbIntegration dbIntegration;

    @Test
    void testGetBatches() {
        var batchEntities = List.of(new BatchEntity(), new BatchEntity(), new BatchEntity());
        when(mockBatchRepository.findAllByCompletedAtBetween(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(batchEntities));

        var result = dbIntegration.getBatches(LocalDate.now(), LocalDate.now(), PageRequest.of(0, 2));

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(3L);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getNumber()).isZero();
        assertThat(result.getTotalPages()).isEqualTo(2);

        verify(mockBatchRepository, times(1)).findAllByCompletedAtBetween(
            any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verifyNoMoreInteractions(mockBatchRepository);
        verifyNoInteractions(mockFileRepository);
    }

    @Test
    void testStoreBatch() {
        var batchEntityCaptor = ArgumentCaptor.forClass(BatchEntity.class);

        var batch = new Batch()
            .withBasename("someBasename")
            .withItems(List.of(
                new Item("something.xml").setStatus(Status.NOT_AN_INVOICE),
                new Item("file1").setStatus(Status.SENT),
                new Item("file1").setStatus(Status.SENT),
                new Item("file1").setStatus(Status.SENT),
                new Item("file1").setStatus(Status.NOT_SENT),
                new Item("file1").setStatus(Status.NOT_SENT)
            ));
        batch.setCompleted();

        dbIntegration.storeBatch(batch);

        verify(mockBatchRepository).save(batchEntityCaptor.capture());
        verify(mockFileRepository).save(any(FileEntity.class));
        verifyNoMoreInteractions(mockBatchRepository, mockFileRepository);

        var batchEntity = batchEntityCaptor.getValue();
        assertThat(batchEntity.getBasename()).isEqualTo(batch.getBasename());
        assertThat(batchEntity.getStartedAt()).isEqualTo(batch.getStartedAt());
        assertThat(batchEntity.getCompletedAt()).isEqualTo(batch.getCompletedAt());
        assertThat(batchEntity.getItems()).hasSize(5);
        assertThat(batchEntity.getTotalItems()).isEqualTo(5L);
        assertThat(batchEntity.getSentItems()).isEqualTo(3L);
    }

    @Test
    void testMapToBatchDto() {
        var batchEntity = new BatchEntity()
            .withId(334455)
            .withBasename("someBasename")
            .withStartedAt(LocalDateTime.now())
            .withCompletedAt(LocalDateTime.now())
            .withTotalItems(234L)
            .withSentItems(123L);

        var batchDto = dbIntegration.mapToBatchDto(batchEntity);

        assertThat(batchDto.id()).isEqualTo(batchEntity.getId());
        assertThat(batchDto.basename()).isEqualTo(batchEntity.getBasename());
        assertThat(batchDto.startedAt()).isEqualTo(batchEntity.getStartedAt());
        assertThat(batchDto.completedAt()).isEqualTo(batchEntity.getCompletedAt());
        assertThat(batchDto.totalItems()).isEqualTo(batchEntity.getTotalItems());
        assertThat(batchDto.sentItems()).isEqualTo(batchEntity.getSentItems());
    }

    @Test
    void testMapToBatchDtoWithNullInput() {
        assertThat(dbIntegration.mapToBatchDto(null)).isNull();
    }
}
