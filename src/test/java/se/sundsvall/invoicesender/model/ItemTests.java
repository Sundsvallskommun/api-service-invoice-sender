package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;

import org.junit.jupiter.api.Test;

class ItemTests {

    private static final String TOTAL_AMOUNT = "someTotalAmount";

    private static final String PAYMENT_REFERENCE = "somePaymentReference";

    private static final String INVOICE_NUMBER = "someInvoiceNumber";

    private static final String INVOICE_DATE = "someInvoiceDate";

    private static final String DUE_DATE = "someDueDate";

    private static final String ACCOUNT_NUMBER = "someAccountNumber";

    private static final String FILENAME = "someFilename";

    private static final String LEGAL_ID = "someLegalId";

    private static final String PARTY_ID = "somePartyId";

    @Test
    void testSettersAndGetters() {
        final var metadata = new Item.Metadata();
        metadata.setAccountNumber(ACCOUNT_NUMBER);
        metadata.setDueDate(DUE_DATE);
        metadata.setInvoiceDate(INVOICE_DATE);
        metadata.setReminder(true);
        metadata.setInvoiceNumber(INVOICE_NUMBER);
        metadata.setPaymentReference(PAYMENT_REFERENCE);
        metadata.setTotalAmount(TOTAL_AMOUNT);

        final var item = new Item();
        item.setFilename(FILENAME);
        item.setStatus(SENT);
        item.setRecipientLegalId(LEGAL_ID);
        item.setRecipientPartyId(PARTY_ID);
        item.setMetadata(metadata);

        assertThat(item.getFilename()).isEqualTo(FILENAME);
        assertThat(item.getStatus()).isEqualTo(SENT);
        assertThat(item.getRecipientLegalId()).isEqualTo(LEGAL_ID);
        assertThat(item.getRecipientPartyId()).isEqualTo(PARTY_ID);
        assertThat(item.getMetadata()).isNotNull().satisfies(itemMetadata -> {
            assertThat(itemMetadata.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(itemMetadata.getDueDate()).isEqualTo(DUE_DATE);
            assertThat(itemMetadata.getInvoiceDate()).isEqualTo(INVOICE_DATE);
            assertThat(itemMetadata.isReminder()).isTrue();
            assertThat(itemMetadata.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
            assertThat(itemMetadata.getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
            assertThat(itemMetadata.getTotalAmount()).isEqualTo(TOTAL_AMOUNT);
        });
    }

    @Test
    void testWithersAndGetters() {
        final var item = new Item()
            .withFilename(FILENAME)
            .withStatus(SENT)
            .withRecipientLegalId(LEGAL_ID)
            .withRecipientPartyId(PARTY_ID)
            .withMetadata(new Item.Metadata()
                .withAccountNumber(ACCOUNT_NUMBER)
                .withDueDate(DUE_DATE)
                .withInvoiceDate(INVOICE_DATE)
                .withReminder(true)
                .withInvoiceNumber(INVOICE_NUMBER)
                .withPaymentReference(PAYMENT_REFERENCE)
                .withTotalAmount(TOTAL_AMOUNT));

        assertThat(item.getFilename()).isEqualTo(FILENAME);
        assertThat(item.getStatus()).isEqualTo(SENT);
        assertThat(item.getRecipientLegalId()).isEqualTo(LEGAL_ID);
        assertThat(item.getRecipientPartyId()).isEqualTo(PARTY_ID);
        assertThat(item.getMetadata()).isNotNull().satisfies(itemMetadata -> {
            assertThat(itemMetadata.getAccountNumber()).isEqualTo(ACCOUNT_NUMBER);
            assertThat(itemMetadata.getDueDate()).isEqualTo(DUE_DATE);
            assertThat(itemMetadata.getInvoiceDate()).isEqualTo(INVOICE_DATE);
            assertThat(itemMetadata.isReminder()).isTrue();
            assertThat(itemMetadata.getInvoiceNumber()).isEqualTo(INVOICE_NUMBER);
            assertThat(itemMetadata.getPaymentReference()).isEqualTo(PAYMENT_REFERENCE);
            assertThat(itemMetadata.getTotalAmount()).isEqualTo(TOTAL_AMOUNT);
        });
    }

    @Test
    void testConstructors() {
        var item = new Item();
        assertThat(item.getFilename()).isNull();

        item = new Item(FILENAME);
        assertThat(item.getFilename()).isEqualTo(FILENAME);
    }
}
