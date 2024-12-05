package se.sundsvall.invoicesender;

import se.sundsvall.invoicesender.integration.db.entity.BatchEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.service.model.Metadata;

import java.util.UUID;
import java.util.function.Consumer;

import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;

public final class TestDataFactory {

	public static ItemEntity createItemEntity() {
		return createItemEntity(null);
	}

	public static ItemEntity createItemEntity(final Consumer<ItemEntity> modifier) {
		var itemEntity = new ItemEntity()
			.withId(123)
			.withFilename("someFilename.jpeg")
			.withRecipientLegalId(UUID.randomUUID().toString())
			.withRecipientPartyId(UUID.randomUUID().toString())
			.withStatus(UNHANDLED)
			.withType(INVOICE)
			.withBatch(createBatchEntity())
			.withMetadata(createMetadata());

		if (modifier != null) {
			modifier.accept(itemEntity);
		}

		return itemEntity;
	}

	public static Metadata createMetadata() {
		return new Metadata()
			.withPayable(true)
			.withInvoiceNumber("123")
			.withTotalAmount("12.34")
			.withDueDate("1986-02-26");
	}

	public static BatchEntity createBatchEntity() {
		return createBatchEntity(null);
	}

	public static BatchEntity createBatchEntity(final Consumer<BatchEntity> modifier) {
		var batch = new BatchEntity()
			.withId(123)
			.withTargetPath("someTargetPath")
			.withLocalPath("someLocalPath")
			.withArchivePath("someArchivePath")
			.withBasename("someBasename")
			.withMunicipalityId("someMunicipalityId")
			.withProcessingEnabled(true);

		if (modifier != null) {
			modifier.accept(batch);
		}

		return batch;
	}

}
