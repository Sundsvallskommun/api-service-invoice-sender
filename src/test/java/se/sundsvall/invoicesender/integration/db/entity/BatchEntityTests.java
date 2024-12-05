package se.sundsvall.invoicesender.integration.db.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BatchEntityTests {

	@Test
	void testWithersAndGetters() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity()
			.withId(12345)
			.withBasename("someBasename")
			.withMunicipalityId("2281")
			.withStartedAt(now)
			.withCompletedAt(now.plusSeconds(30L))
			.withItems(List.of(new ItemEntity()))
			.withSentItems(456L)
			.withTotalItems(789L)
			.withIgnoredItems(123L)
			.withProcessingEnabled(true)
			.withCompleted(true)
			.withData(new byte[] {
				1, 2, 3
			})
			.withArchivePath("someArchivePath")
			.withLocalPath("someLocalPath")
			.withTargetPath("someTargetPath");

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		assertThat(batchEntity.isProcessingEnabled()).isTrue();
		assertThat(batchEntity.isCompleted()).isTrue();
		assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batchEntity.getLocalPath()).isEqualTo("someLocalPath");
		assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
	}

	@Test
	void testSettersAndGetters() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity();
		batchEntity.setId(12345);
		batchEntity.setBasename("someBasename");
		batchEntity.setMunicipalityId("2281");
		batchEntity.setStartedAt(now);
		batchEntity.setCompletedAt(now.plusSeconds(30L));
		batchEntity.setItems(List.of(new ItemEntity()));
		batchEntity.setSentItems(456L);
		batchEntity.setTotalItems(789L);
		batchEntity.setIgnoredItems(123L);
		batchEntity.setProcessingEnabled(true);
		batchEntity.setCompleted(true);
		batchEntity.setData(new byte[] {
			1, 2, 3
		});
		batchEntity.setArchivePath("someArchivePath");
		batchEntity.setLocalPath("someLocalPath");
		batchEntity.setTargetPath("someTargetPath");

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		assertThat(batchEntity.isProcessingEnabled()).isTrue();
		assertThat(batchEntity.isCompleted()).isTrue();
		assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batchEntity.getLocalPath()).isEqualTo("someLocalPath");
		assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
	}
}
