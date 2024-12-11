package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
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
		Assertions.assertThat(new BatchEntity().getStartedAt()).isNotNull();

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

		Assertions.assertThat(batchEntity.getId()).isEqualTo(12345);
		Assertions.assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		Assertions.assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		Assertions.assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		Assertions.assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		Assertions.assertThat(batchEntity.getItems()).hasSize(1);
		Assertions.assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		Assertions.assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		Assertions.assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		Assertions.assertThat(batchEntity.isProcessingEnabled()).isTrue();
		Assertions.assertThat(batchEntity.isCompleted()).isTrue();
		Assertions.assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		Assertions.assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		Assertions.assertThat(batchEntity.getLocalPath()).isEqualTo("someLocalPath");
		Assertions.assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
	}

	@Test
	void testSettersAndGetters() {
		Assertions.assertThat(new BatchEntity().getStartedAt()).isNotNull();

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

		Assertions.assertThat(batchEntity.getId()).isEqualTo(12345);
		Assertions.assertThat(batchEntity.getBasename()).isEqualTo("someBasename");
		Assertions.assertThat(batchEntity.getMunicipalityId()).isEqualTo("2281");
		Assertions.assertThat(batchEntity.getStartedAt()).isEqualTo(now);
		Assertions.assertThat(batchEntity.getCompletedAt()).isEqualTo(now.plusSeconds(30L));
		Assertions.assertThat(batchEntity.getItems()).hasSize(1);
		Assertions.assertThat(batchEntity.getSentItems()).isEqualTo(456L);
		Assertions.assertThat(batchEntity.getTotalItems()).isEqualTo(789L);
		Assertions.assertThat(batchEntity.getIgnoredItems()).isEqualTo(123L);
		Assertions.assertThat(batchEntity.isProcessingEnabled()).isTrue();
		Assertions.assertThat(batchEntity.isCompleted()).isTrue();
		Assertions.assertThat(batchEntity.getData()).containsExactly(1, 2, 3);
		Assertions.assertThat(batchEntity.getArchivePath()).isEqualTo("someArchivePath");
		Assertions.assertThat(batchEntity.getLocalPath()).isEqualTo("someLocalPath");
		Assertions.assertThat(batchEntity.getTargetPath()).isEqualTo("someTargetPath");
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
				Arguments.of(new BatchEntity().withBasename("123"), new BatchEntity().withBasename("321"), false),
				Arguments.of(new BatchEntity(), "someString", false),
				Arguments.of(new BatchEntity().withId(321).withBasename("baseName1"), new BatchEntity().withId(321).withBasename("baseName2"), true));
		}
	}
}
