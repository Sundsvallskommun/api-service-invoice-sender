package se.sundsvall.invoicesender.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BatchTests {

    @Test
    void testGettersAndSetters() {
        var batch = new Batch()
            .withBasename("someSevenZipFilename")
            .withPath("somePath")
            .withRemotePath("someRemotePath")
            .withItems(List.of(new Item("someItem"), new Item("someOtherItem")));

        assertThat(batch.getBasename()).isEqualTo("someSevenZipFilename");
        assertThat(batch.getPath()).isEqualTo("somePath");
        assertThat(batch.getRemotePath()).isEqualTo("someRemotePath");
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
