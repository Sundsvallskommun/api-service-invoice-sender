package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.invoicesender.model.Status;

class ItemEntityTests {

    @Test
    void testWithersAndGetters() {
        var itemEntity = new ItemEntity()
            .withId(12345)
            .withFilename("someFilename")
            .withStatus(Status.NOT_SENT);

        assertThat(itemEntity.getId()).isEqualTo(12345);
        assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
        assertThat(itemEntity.getStatus()).isEqualTo(Status.NOT_SENT);
    }

    @Test
    void testSettersAndGetters() {
        var itemEntity = new ItemEntity();
        itemEntity.setId(12345);
        itemEntity.setFilename("someFilename");
        itemEntity.setStatus(Status.NOT_SENT);

        assertThat(itemEntity.getId()).isEqualTo(12345);
        assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
        assertThat(itemEntity.getStatus()).isEqualTo(Status.NOT_SENT);
    }
}
