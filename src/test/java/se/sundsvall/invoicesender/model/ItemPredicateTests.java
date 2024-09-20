package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.model.Item.ITEM_HAS_LEGAL_ID;
import static se.sundsvall.invoicesender.model.Item.ITEM_HAS_PARTY_ID;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_AN_INVOICE;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_A_PDF;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_IGNORED;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_PROCESSABLE;
import static se.sundsvall.invoicesender.model.Item.ITEM_IS_SENT;
import static se.sundsvall.invoicesender.model.Item.ITEM_LACKS_METADATA;
import static se.sundsvall.invoicesender.model.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.model.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.model.ItemType.INVOICE;
import static se.sundsvall.invoicesender.model.ItemType.OTHER;

import org.junit.jupiter.api.Test;

class ItemPredicateTests {

    private static final String DUMMY_DOT_PDF = "dummy.pdf";
    private static final String DUMMY_DOT_DOCX = "dummy.docx";

    @Test
    void test_ITEM_IS_A_PDF() {
        var pdfItem = new Item(DUMMY_DOT_PDF);
        var nonPdfItem = new Item(DUMMY_DOT_DOCX);

        assertThat(ITEM_IS_A_PDF.test(pdfItem)).isTrue();
        assertThat(ITEM_IS_A_PDF.test(nonPdfItem)).isFalse();
    }

    @Test
    void test_ITEM_IS_AN_INVOICE() {
        var invoiceItem = new Item(DUMMY_DOT_PDF).withType(INVOICE);
        var nonInvoiceItem = new Item(DUMMY_DOT_DOCX).withType(OTHER);

        assertThat(ITEM_IS_AN_INVOICE.test(invoiceItem)).isTrue();
        assertThat(ITEM_IS_AN_INVOICE.test(nonInvoiceItem)).isFalse();
    }

    @Test
    void test_ITEM_IS_IGNORED() {
        var item = new Item(DUMMY_DOT_PDF).withStatus(UNHANDLED);
        var ignoredItem = new Item(DUMMY_DOT_DOCX).withStatus(IGNORED);

        assertThat(ITEM_IS_IGNORED.test(item)).isFalse();
        assertThat(ITEM_IS_IGNORED.test(ignoredItem)).isTrue();
    }

    @Test
    void test_ITEM_LACKS_METADATA() {
        var itemWithMetadata = new Item(DUMMY_DOT_PDF).withStatus(UNHANDLED);
        var itemWithoutMetadata = new Item(DUMMY_DOT_DOCX).withStatus(METADATA_INCOMPLETE);

        assertThat(ITEM_LACKS_METADATA.test(itemWithMetadata)).isFalse();
        assertThat(ITEM_LACKS_METADATA.test(itemWithoutMetadata)).isTrue();
    }

    @Test
    void test_ITEM_IS_PROCESSABLE() {
        var processableInvoiceItem = new Item(DUMMY_DOT_PDF).withType(INVOICE).withStatus(UNHANDLED);
        var ignoredItem = new Item(DUMMY_DOT_DOCX).withStatus(IGNORED);
        var itemWithoutMetadata = new Item(DUMMY_DOT_DOCX).withStatus(METADATA_INCOMPLETE);

        assertThat(ITEM_IS_PROCESSABLE.test(processableInvoiceItem)).isTrue();
        assertThat(ITEM_IS_PROCESSABLE.test(ignoredItem)).isFalse();
        assertThat(ITEM_IS_PROCESSABLE.test(itemWithoutMetadata)).isFalse();
    }

    @Test
    void test_ITEM_HAS_LEGAL_ID() {
        var item1 = new Item(DUMMY_DOT_PDF).withStatus(RECIPIENT_LEGAL_ID_FOUND);
        var item2 = new Item(DUMMY_DOT_PDF).withStatus(UNHANDLED);

        assertThat(ITEM_HAS_LEGAL_ID.test(item1)).isTrue();
        assertThat(ITEM_HAS_LEGAL_ID.test(item2)).isFalse();
    }

    @Test
    void test_ITEM_HAS_PARTY_ID() {
        var item1 = new Item(DUMMY_DOT_PDF).withStatus(RECIPIENT_PARTY_ID_FOUND);
        var item2 = new Item(DUMMY_DOT_PDF).withStatus(UNHANDLED);

        assertThat(ITEM_HAS_PARTY_ID.test(item1)).isTrue();
        assertThat(ITEM_HAS_PARTY_ID.test(item2)).isFalse();
    }

    @Test
    void test_ITEM_IS_SENT() {
        var item1 = new Item(DUMMY_DOT_PDF).withStatus(SENT);
        var item2 = new Item(DUMMY_DOT_PDF).withStatus(UNHANDLED);

        assertThat(ITEM_IS_SENT.test(item1)).isTrue();
        assertThat(ITEM_IS_SENT.test(item2)).isFalse();
    }
}
