package se.sundsvall.invoicesender.util;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.OTHER;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.INVOICE_COULD_NOT_BE_SENT;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_AN_INVOICE;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_A_PDF;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_IGNORED;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_IS_NOT_PROCESSABLE;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.ITEM_LACKS_METADATA;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.RECIPIENT_HAS_INVALID_LEGAL_ID;
import static se.sundsvall.invoicesender.service.model.ItemPredicate.RECIPIENT_HAS_INVALID_PARTY_ID;

import org.junit.jupiter.api.Test;

class PredicateTests {

	private static final String DUMMY_DOT_PDF = "dummy.pdf";
	private static final String DUMMY_DOT_DOCX = "dummy.docx";

	@Test
	void test_ITEM_IS_A_PDF() {
		var pdfItem = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF));
		var nonPdfItem = createItemEntity(item -> item.setFilename(DUMMY_DOT_DOCX));

		assertThat(ITEM_IS_A_PDF.test(pdfItem)).isTrue();
		assertThat(ITEM_IS_A_PDF.test(nonPdfItem)).isFalse();
	}

	@Test
	void test_ITEM_IS_AN_INVOICE() {
		var invoiceItem = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withType(INVOICE);
		var nonInvoiceItem = createItemEntity(item -> item.setFilename(DUMMY_DOT_DOCX)).withType(OTHER);

		assertThat(ITEM_IS_AN_INVOICE.test(invoiceItem)).isTrue();
		assertThat(ITEM_IS_AN_INVOICE.test(nonInvoiceItem)).isFalse();
	}

	@Test
	void test_ITEM_IS_IGNORED() {
		var item = createItemEntity(item1 -> item1.setFilename(DUMMY_DOT_PDF)).withStatus(UNHANDLED);
		var ignoredItem = createItemEntity(item1 -> item1.setFilename(DUMMY_DOT_DOCX)).withStatus(IGNORED);

		assertThat(ITEM_IS_IGNORED.test(item)).isFalse();
		assertThat(ITEM_IS_IGNORED.test(ignoredItem)).isTrue();
	}

	@Test
	void test_ITEM_LACKS_METADATA() {
		var itemWithMetadata = createItemEntity(item1 -> item1.setFilename(DUMMY_DOT_PDF)).withStatus(UNHANDLED);
		var itemWithoutMetadata = createItemEntity(item1 -> item1.setFilename(DUMMY_DOT_DOCX)).withStatus(METADATA_INCOMPLETE);

		assertThat(ITEM_LACKS_METADATA.test(itemWithMetadata)).isFalse();
		assertThat(ITEM_LACKS_METADATA.test(itemWithoutMetadata)).isTrue();
	}

	@Test
	void test_ITEM_IS_NOT_PROCESSABLE() {
		var processableInvoice = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withType(INVOICE);
		var unProcessableInvoice1 = createItemEntity(item -> item.setFilename(DUMMY_DOT_DOCX));
		var unProcessableInvoice2 = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(IGNORED);
		var unProcessableInvoice3 = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(METADATA_INCOMPLETE);

		assertThat(ITEM_IS_NOT_PROCESSABLE.test(processableInvoice)).isFalse();
		assertThat(ITEM_IS_NOT_PROCESSABLE.test(unProcessableInvoice1)).isTrue();
		assertThat(ITEM_IS_NOT_PROCESSABLE.test(unProcessableInvoice2)).isTrue();
		assertThat(ITEM_IS_NOT_PROCESSABLE.test(unProcessableInvoice3)).isTrue();
	}

	@Test
	void test_RECIPIENT_HAS_INVALID_LEGAL_ID() {
		var invalidRecipientLegalId = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		var validRecipientLegalId = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(UNHANDLED);

		assertThat(RECIPIENT_HAS_INVALID_LEGAL_ID.test(invalidRecipientLegalId)).isTrue();
		assertThat(RECIPIENT_HAS_INVALID_LEGAL_ID.test(validRecipientLegalId)).isFalse();
	}

	@Test
	void test_RECIPIENT_HAS_INVALID_PARTY_ID() {
		var invalidRecipientPartyId = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(RECIPIENT_PARTY_ID_NOT_FOUND);
		var validRecipientPartyId = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(UNHANDLED);

		assertThat(RECIPIENT_HAS_INVALID_PARTY_ID.test(invalidRecipientPartyId)).isTrue();
		assertThat(RECIPIENT_HAS_INVALID_PARTY_ID.test(validRecipientPartyId)).isFalse();
	}

	@Test
	void test_INVOICE_COULD_NOT_BE_SENT() {
		var invoiceCouldNotBeSent = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(NOT_SENT);
		var invoiceSent = createItemEntity(item -> item.setFilename(DUMMY_DOT_PDF)).withStatus(SENT);

		assertThat(INVOICE_COULD_NOT_BE_SENT.test(invoiceCouldNotBeSent)).isTrue();
		assertThat(INVOICE_COULD_NOT_BE_SENT.test(invoiceSent)).isFalse();
	}

}
