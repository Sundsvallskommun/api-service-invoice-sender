package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class BatchEntityTests {

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new BatchEntity()).hasAllNullFieldsOrPropertiesExcept("startedAt", "totalItems", "items", "ignoredItems", "sentItems", "processingEnabled", "completed");
	}

	@Test
	void testBuilderPattern() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var date = LocalDate.now();
		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity()
			.withId(12345)
			.withMunicipalityId("2281")
			.withFilename("someBasename")
			.withStartedAt(now)
			.withCompletedAt(now.plusSeconds(30L))
			.withItems(List.of(new ItemEntity()))
			.withSentItems(456L)
			.withTotalItems(789L)
			.withIgnoredItems(123L)
			.withProcessingEnabled(true)
			.withCompleted(true)
			.withDate(date)
			.withData(new byte[] {
				1, 2, 3
			})
			.withArchivePath("someArchivePath")
			.withTargetPath("someTargetPath");

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getFilename()).isEqualTo("someBasename");
		assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		assertThat(batchEntity.isProcessingEnabled()).isTrue();
		assertThat(batchEntity.isCompleted()).isTrue();
		assertThat(batchEntity.getDate()).isEqualTo(date);
		assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
	}

	@Test
	void testSettersAndGetters() {
		assertThat(new BatchEntity().getStartedAt()).isNotNull();

		var date = LocalDate.now();
		var now = LocalDateTime.now();

		var batchEntity = new BatchEntity();
		batchEntity.setId(12345);
		batchEntity.setFilename("someBasename");
		batchEntity.setMunicipalityId("2281");
		batchEntity.setStartedAt(now);
		batchEntity.setCompletedAt(now.plusSeconds(30L));
		batchEntity.setItems(List.of(new ItemEntity()));
		batchEntity.setSentItems(456L);
		batchEntity.setTotalItems(789L);
		batchEntity.setIgnoredItems(123L);
		batchEntity.setDate(date);
		batchEntity.setProcessingEnabled(true);
		batchEntity.setCompleted(true);
		batchEntity.setData(new byte[] {
			1, 2, 3
		});
		batchEntity.setArchivePath("someArchivePath");
		batchEntity.setTargetPath("someTargetPath");

		assertThat(batchEntity.getId()).isEqualTo(12345);
		assertThat(batchEntity.getFilename()).isEqualTo("someBasename");
		assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		assertThat(batchEntity.getItems()).hasSize(1);
		assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		assertThat(batchEntity.getDate()).isEqualTo(date);
		assertThat(batchEntity.isProcessingEnabled()).isTrue();
		assertThat(batchEntity.isCompleted()).isTrue();
		assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
	}

	@ParameterizedTest
	@ArgumentsSource(EqualsArgumentsProvider.class)
	void testEquals(final Object first, final Object second, final boolean shouldEqual) {
		if (shouldEqual) {
			assertThat(first).isEqualTo(second);
		} else {
			assertThat(first).isNotEqualTo(second);
		}
	}

	@Test
	void testHashCode() {
		assertThat(new BatchEntity()).hasSameHashCodeAs(BatchEntity.class);
	}

	private static class EqualsArgumentsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {

			return Stream.of(
				Arguments.of(new BatchEntity(), new BatchEntity(), false),
				Arguments.of(new BatchEntity().withId(123), new BatchEntity().withId(123), true),
				Arguments.of(new BatchEntity().withFilename("123"), new BatchEntity().withFilename("321"), false),
				Arguments.of(new BatchEntity(), "someString", false),
				Arguments.of(new BatchEntity().withId(321).withFilename("baseName1"), new BatchEntity().withId(321).withFilename("baseName2"), true));
		}
	}
}
