package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import se.sundsvall.invoicesender.model.ItemStatus;

class ItemEntityTests {

    @Test
    void testWithersAndGetters() {
        var itemEntity = new ItemEntity()
            .withId(12345)
            .withFilename("someFilename")
            .withStatus(ItemStatus.NOT_SENT);

        assertThat(itemEntity.getId()).isEqualTo(12345);
        assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
        assertThat(itemEntity.getStatus()).isEqualTo(ItemStatus.NOT_SENT);
    }

    @Test
    void testSettersAndGetters() {
        var itemEntity = new ItemEntity();
        itemEntity.setId(12345);
        itemEntity.setFilename("someFilename");
        itemEntity.setStatus(ItemStatus.NOT_SENT);

        assertThat(itemEntity.getId()).isEqualTo(12345);
        assertThat(itemEntity.getFilename()).isEqualTo("someFilename");
        assertThat(itemEntity.getStatus()).isEqualTo(ItemStatus.NOT_SENT);
    }
}
