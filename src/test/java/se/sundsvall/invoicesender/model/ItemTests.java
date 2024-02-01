package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;

import org.junit.jupiter.api.Test;

class ItemTests {

    @Test
    void testSettersAndGetters() {
        var metadata = new Item.Metadata();
        metadata.setAccountNumber("someAccountNumber");
        metadata.setDueDate("someDueDate");
        metadata.setInvoiceDate("someInvoiceDate");
        metadata.setReminder(true);
        metadata.setInvoiceNumber("someInvoiceNumber");
        metadata.setPaymentReference("somePaymentReference");
        metadata.setTotalAmount("someTotalAmount");

        var item = new Item();
        item.setFilename("someFilename");
        item.setStatus(SENT);
        item.setRecipientLegalId("someLegalId");
        item.setRecipientPartyId("somePartyId");
        item.setMetadata(metadata);

        assertThat(item.getFilename()).isEqualTo("someFilename");
        assertThat(item.getStatus()).isEqualTo(SENT);
        assertThat(item.getRecipientLegalId()).isEqualTo("someLegalId");
        assertThat(item.getRecipientPartyId()).isEqualTo("somePartyId");
        assertThat(item.getMetadata()).isNotNull().satisfies(itemMetadata -> {
            assertThat(itemMetadata.getAccountNumber()).isEqualTo("someAccountNumber");
            assertThat(itemMetadata.getDueDate()).isEqualTo("someDueDate");
            assertThat(itemMetadata.getInvoiceDate()).isEqualTo("someInvoiceDate");
            assertThat(itemMetadata.isReminder()).isTrue();
            assertThat(itemMetadata.getInvoiceNumber()).isEqualTo("someInvoiceNumber");
            assertThat(itemMetadata.getPaymentReference()).isEqualTo("somePaymentReference");
            assertThat(itemMetadata.getTotalAmount()).isEqualTo("someTotalAmount");
        });
    }


    @Test
    void testWithersAndGetters() {
        var item = new Item()
            .withFilename("someFilename")
            .withStatus(SENT)
            .withRecipientLegalId("someLegalId")
            .withRecipientPartyId("somePartyId")
            .withMetadata(new Item.Metadata()
                .withAccountNumber("someAccountNumber")
                .withDueDate("someDueDate")
                .withInvoiceDate("someInvoiceDate")
                .withReminder(true)
                .withInvoiceNumber("someInvoiceNumber")
                .withPaymentReference("somePaymentReference")
                .withTotalAmount("someTotalAmount"));

        assertThat(item.getFilename()).isEqualTo("someFilename");
        assertThat(item.getStatus()).isEqualTo(SENT);
        assertThat(item.getRecipientLegalId()).isEqualTo("someLegalId");
        assertThat(item.getRecipientPartyId()).isEqualTo("somePartyId");
        assertThat(item.getMetadata()).isNotNull().satisfies(itemMetadata -> {
            assertThat(itemMetadata.getAccountNumber()).isEqualTo("someAccountNumber");
            assertThat(itemMetadata.getDueDate()).isEqualTo("someDueDate");
            assertThat(itemMetadata.getInvoiceDate()).isEqualTo("someInvoiceDate");
            assertThat(itemMetadata.isReminder()).isTrue();
            assertThat(itemMetadata.getInvoiceNumber()).isEqualTo("someInvoiceNumber");
            assertThat(itemMetadata.getPaymentReference()).isEqualTo("somePaymentReference");
            assertThat(itemMetadata.getTotalAmount()).isEqualTo("someTotalAmount");
        });
    }

    @Test
    void testConstructors() {
        var item = new Item();
        assertThat(item.getFilename()).isNull();

        item = new Item("someFilename");
        assertThat(item.getFilename()).isEqualTo("someFilename");
    }
}
