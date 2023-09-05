package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;

import org.junit.jupiter.api.Test;

class ItemTests {

    @Test
    void testGettersAndSetters() {
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
        item.setStatus(NOT_AN_INVOICE);
        item.setRecipientLegalId("someLegalId");
        item.setRecipientPartyId("somePartyId");
        item.setMetadata(metadata);

        assertThat(item.getFilename()).isEqualTo("someFilename");
        assertThat(item.getStatus()).isEqualTo(NOT_AN_INVOICE);
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
