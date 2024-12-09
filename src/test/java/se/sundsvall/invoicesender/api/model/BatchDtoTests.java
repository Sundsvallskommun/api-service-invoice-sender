package se.sundsvall.invoicesender.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class BatchDtoTests {

	@Test
	void testConstructorAndGetters() {
		var now = LocalDateTime.now();

		var batchDto = new BatchDto(112233, "someBasename", now, now.plusSeconds(12L), 12L, 10L, false);

		assertThat(batchDto.id()).isEqualTo(112233);
		assertThat(batchDto.basename()).isEqualTo("someBasename");
		assertThat(batchDto.startedAt()).isEqualTo(now);
		assertThat(batchDto.completedAt()).isEqualTo(now.plusSeconds(12L));
		assertThat(batchDto.totalItems()).isEqualTo(12L);
		assertThat(batchDto.sentItems()).isEqualTo(10L);
		assertThat(batchDto.processingEnabled()).isFalse();
	}
}
