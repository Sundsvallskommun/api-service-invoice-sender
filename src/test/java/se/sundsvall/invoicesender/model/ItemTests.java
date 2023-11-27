package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.model.Status.NOT_AN_INVOICE;

import org.junit.jupiter.api.Test;

class ItemTests {

    @Test
    void testGettersAndSetters() {
        var item = new Item()
            .setZipFilename("someZipFilename")
            .setFilename("someFilename")
            .setStatus(NOT_AN_INVOICE)
            .setRecipientLegalId("someLegalId")
            .setRecipientPartyId("somePartyId");

        assertThat(item.getZipFilename()).isEqualTo("someZipFilename");
        assertThat(item.getFilename()).isEqualTo("someFilename");
        assertThat(item.getStatus()).isEqualTo(NOT_AN_INVOICE);
        assertThat(item.getRecipientLegalId()).isEqualTo("someLegalId");
        assertThat(item.getRecipientPartyId()).isEqualTo("somePartyId");
    }
}
