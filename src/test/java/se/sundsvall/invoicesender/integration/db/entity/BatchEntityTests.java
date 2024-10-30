package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class BatchEntityTests {

	@Test
	void testWithersAndGetters() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity()
			.withId(12345)
			.withBasename("someBasename")
			.withStartedAt(now)
			.withCompletedAt(now.plusSeconds(30L))
			.withItems(List.of(new ItemEntity()))
			.withSentItems(456L)
			.withTotalItems(789L);

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
	}

	@Test
	void testSettersAndGetters() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity();
		batchEntity.setId(12345);
		batchEntity.setBasename("someBasename");
		batchEntity.setStartedAt(now);
		batchEntity.setCompletedAt(now.plusSeconds(30L));
		batchEntity.setItems(List.of(new ItemEntity()));
		batchEntity.setSentItems(456L);
		batchEntity.setTotalItems(789L);

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
	}
}
