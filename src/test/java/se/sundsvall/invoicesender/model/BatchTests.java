package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BatchTests {

    @Test
    void testWithersAndGetters() {
        var batch = new Batch()
            .withBasename("someSevenZipFilename")
            .withData("data".getBytes())
            .withPath("somePath")
            .withItems(List.of(new Item("someItem"), new Item("someOtherItem")));

        assertThat(batch.getBasename()).isEqualTo("someSevenZipFilename");
        assertThat(batch.getData()).hasSize(4);
        assertThat(batch.getPath()).isEqualTo("somePath");
        assertThat(batch.getItems()).hasSize(2);
        assertThat(batch.getStartedAt()).isNotNull();
        assertThat(batch.getCompletedAt()).isNull();
    }

    @Test
    void testSettersAndGetters() {
        var batch = new Batch();
        batch.setBasename("someSevenZipFilename");
        batch.setData("data".getBytes());
        batch.setPath("somePath");
        batch.setItems(List.of(new Item("someItem"), new Item("someOtherItem")));

        assertThat(batch.getBasename()).isEqualTo("someSevenZipFilename");
        assertThat(batch.getData()).hasSize(4);
        assertThat(batch.getPath()).isEqualTo("somePath");
        assertThat(batch.getItems()).hasSize(2);
        assertThat(batch.getStartedAt()).isNotNull();
        assertThat(batch.getCompletedAt()).isNull();

        batch.setCompleted();
        assertThat(batch.getCompletedAt()).isNotNull();
    }

    @Test
    void testAddItem() {
        var batch = new Batch();
        assertThat(batch.getItems()).isNull();

        batch.addItem(new Item("someItem"));
        assertThat(batch.getItems()).hasSize(1);
    }
}
