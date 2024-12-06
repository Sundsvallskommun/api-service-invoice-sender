package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import se.sundsvall.invoicesender.service.model.Metadata;

class ItemEntityTests {

	@Test
	void testNoDirtOnCreatedBean() {
		assertThat(new ItemEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testBuilderPattern() {
		var batchEntity = new BatchEntity();
		var metadata = new Metadata();

		var itemEntity = new ItemEntity()
			.withId(12345)
			.withFilename("someFilename")
			.withStatus(NOT_SENT)
			.withType(INVOICE)
			.withBatch(batchEntity)
			.withMetadata(metadata)
			.withRecipientLegalId("someRecipientLegalId")
			.withRecipientPartyId("someRecipientPartyId");

		assertThat(itemEntity.getId()).isEqualTo(12345);
		assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
		assertThat(itemEntity.getStatus()).isEqualTo(NOT_SENT);
		assertThat(itemEntity.getType()).isEqualTo(INVOICE);
		assertThat(itemEntity.getBatch()).isEqualTo(batchEntity);
		assertThat(itemEntity.getMetadata()).isEqualTo(metadata);
		assertThat(itemEntity.getRecipientLegalId()).isEqualTo("someRecipientLegalId");
		assertThat(itemEntity.getRecipientPartyId()).isEqualTo("someRecipientPartyId");
	}

	@Test
	void testSettersAndGetters() {
		var batchEntity = new BatchEntity();
		var metadata = new Metadata();

		var itemEntity = new ItemEntity();
		itemEntity.setId(12345);
		itemEntity.setFilename("someFilename");
		itemEntity.setStatus(NOT_SENT);
		itemEntity.setType(INVOICE);
		itemEntity.setRecipientLegalId("someRecipientLegalId");
		itemEntity.setRecipientPartyId("someRecipientPartyId");
		itemEntity.setBatch(batchEntity);
		itemEntity.setMetadata(metadata);

		assertThat(itemEntity.getId()).isEqualTo(12345);
		assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
		assertThat(itemEntity.getStatus()).isEqualTo(NOT_SENT);
		assertThat(itemEntity.getType()).isEqualTo(INVOICE);
		assertThat(itemEntity.getBatch()).isEqualTo(batchEntity);
		assertThat(itemEntity.getMetadata()).isEqualTo(metadata);
		assertThat(itemEntity.getRecipientLegalId()).isEqualTo("someRecipientLegalId");
		assertThat(itemEntity.getRecipientPartyId()).isEqualTo("someRecipientPartyId");
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
		assertThat(new ItemEntity().hashCode()).isEqualTo(ItemEntity.class.hashCode());
	}

	private static class EqualsArgumentsProvider implements ArgumentsProvider {

		@Override
		public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {

			return Stream.of(
				Arguments.of(new ItemEntity(), new ItemEntity(), false),
				Arguments.of(new ItemEntity().withId(123), new ItemEntity().withId(123), true),
				Arguments.of(new ItemEntity().withFilename("fileName1"), new ItemEntity().withFilename("fileName2"), false),
				Arguments.of(new ItemEntity(), "someString", false),
				Arguments.of(new ItemEntity().withId(321).withFilename("fileName1"), new ItemEntity().withId(321).withFilename("fileName2"), true));
		}
	}
}
