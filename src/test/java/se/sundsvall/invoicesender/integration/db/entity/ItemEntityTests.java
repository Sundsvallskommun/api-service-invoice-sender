package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.invoicesender.model.Status;

class ItemEntityTests {

    @Test
    void testGettersAndSetters() {
        var itemEntity = new ItemEntity();
        itemEntity.withId(12345);
        itemEntity.withFilename("someFilename");
        itemEntity.withStatus(Status.NOT_SENT);

        assertThat(itemEntity.getId()).isEqualTo(12345);
        assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
        assertThat(itemEntity.getStatus()).isEqualTo(Status.NOT_SENT);
    }
}
