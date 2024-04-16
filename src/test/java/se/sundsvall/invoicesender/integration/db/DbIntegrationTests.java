package se.sundsvall.invoicesender.integration.db;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.invoicesender.model.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.model.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.model.ItemType.INVOICE;
import static se.sundsvall.invoicesender.model.ItemType.OTHER;

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
import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;

@ExtendWith(MockitoExtension.class)
class DbIntegrationTests {

    @Mock
    private BatchRepository mockBatchRepository;
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
    }

    @Test
    void testStoreBatch() {
        var batchEntityCaptor = ArgumentCaptor.forClass(BatchEntity.class);

        var batch = new Batch()
            .withBasename("someBasename")
            .withItems(List.of(
                new Item("something.xml").withType(OTHER).withStatus(UNHANDLED),
                new Item("file1").withType(INVOICE).withStatus(SENT),
                new Item("file2").withType(INVOICE).withStatus(SENT),
                new Item("file3").withType(INVOICE).withStatus(IGNORED),
                new Item("file4").withType(INVOICE).withStatus(NOT_SENT),
                new Item("file5").withType(INVOICE).withStatus(NOT_SENT)
            ));
        batch.setCompleted();

        dbIntegration.storeBatch(batch);

        verify(mockBatchRepository).save(batchEntityCaptor.capture());

        var batchEntity = batchEntityCaptor.getValue();
        assertThat(batchEntity.getBasename()).isEqualTo(batch.getBasename());
        assertThat(batchEntity.getStartedAt()).isEqualTo(batch.getStartedAt());
        assertThat(batchEntity.getCompletedAt()).isEqualTo(batch.getCompletedAt());
        assertThat(batchEntity.getItems()).hasSize(5);
        assertThat(batchEntity.getTotalItems()).isEqualTo(4L);
        assertThat(batchEntity.getIgnoredItems()).isEqualTo(1L);
        assertThat(batchEntity.getSentItems()).isEqualTo(2L);
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

        var batchDto = dbIntegration.mapToBatchDto(batchEntity, true);

        assertThat(batchDto.id()).isEqualTo(batchEntity.getId());
        assertThat(batchDto.basename()).isEqualTo(batchEntity.getBasename());
        assertThat(batchDto.startedAt()).isEqualTo(batchEntity.getStartedAt());
        assertThat(batchDto.completedAt()).isEqualTo(batchEntity.getCompletedAt());
        assertThat(batchDto.totalItems()).isEqualTo(batchEntity.getTotalItems());
        assertThat(batchDto.sentItems()).isEqualTo(batchEntity.getSentItems());
        assertThat(batchDto.processingEnabled()).isTrue();
    }

    @Test
    void testMapToBatchDtoWithNullInput() {
        assertThat(dbIntegration.mapToBatchDto(null, false)).isNull();
    }
}
