package se.sundsvall.invoicesender.integration.db.entity;

import org.junit.jupiter.api.Test;
import se.sundsvall.invoicesender.service.model.Metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;

class ItemEntityTests {

	@Test
	void testWithersAndGetters() {
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
}
