package se.sundsvall.invoicesender.integration.db.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileEntityTests {

    @Test
    void testWithersAndGetters() {
        var fileEntity = new FileEntity()
            .withId(12345)
            .withFilename("someFilename")
            .withBatch(new BatchEntity())
            .withData("data".getBytes());
        
        assertThat(fileEntity.getId()).isEqualTo(12345);
        assertThat(fileEntity.getFilename()).isEqualTo("someFilename");
        assertThat(fileEntity.getBatch()).isNotNull();
        assertThat(fileEntity.getData()).hasSize(4);
    }

    @Test
    void testSettersAndGetters() {
        var fileEntity = new FileEntity();
        fileEntity.setId(12345);
        fileEntity.setFilename("someFilename");
        fileEntity.setBatch(new BatchEntity());
        fileEntity.setData("data".getBytes());

        assertThat(fileEntity.getId()).isEqualTo(12345);
        assertThat(fileEntity.getFilename()).isEqualTo("someFilename");
        assertThat(fileEntity.getBatch()).isNotNull();
        assertThat(fileEntity.getData()).hasSize(4);
    }
}
