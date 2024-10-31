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
			.withLocalPath("somePath")
			.withArchivePath("someArchivePath")
			.withTargetPath("someTargetPath")
			.withProcess(true)
			.withItems(List.of(new Item("someItem"), new Item("someOtherItem")));

		assertThat(batch.getBasename()).isEqualTo("someSevenZipFilename");
		assertThat(batch.getData()).hasSize(4);
		assertThat(batch.getLocalPath()).isEqualTo("somePath");
		assertThat(batch.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batch.getTargetPath()).isEqualTo("someTargetPath");
		assertThat(batch.isProcessingEnabled()).isTrue();
		assertThat(batch.getItems()).hasSize(2);
		assertThat(batch.getStartedAt()).isNotNull();
		assertThat(batch.getCompletedAt()).isNull();
	}

	@Test
	void testSettersAndGetters() {
		var batch = new Batch();
		batch.setBasename("someSevenZipFilename");
		batch.setData("data".getBytes());
		batch.setLocalPath("somePath");
		batch.setArchivePath("someArchivePath");
		batch.setTargetPath("someTargetPath");
		batch.setProcessingEnabled(true);
		batch.setItems(List.of(new Item("someItem"), new Item("someOtherItem")));

		assertThat(batch.getBasename()).isEqualTo("someSevenZipFilename");
		assertThat(batch.getData()).hasSize(4);
		assertThat(batch.getLocalPath()).isEqualTo("somePath");
		assertThat(batch.getArchivePath()).isEqualTo("someArchivePath");
		assertThat(batch.getTargetPath()).isEqualTo("someTargetPath");
		assertThat(batch.isProcessingEnabled()).isTrue();
		assertThat(batch.getItems()).hasSize(2);
		assertThat(batch.getStartedAt()).isNotNull();
		assertThat(batch.getCompletedAt()).isNull();

		batch.setCompleted();
		assertThat(batch.getCompletedAt()).isNotNull();
	}

	@Test
	void addItem() {
		var batch = new Batch();

		batch.addItem(new Item("someItem"));
		assertThat(batch.getItems()).hasSize(1);
	}

	@Test
	void removeItem() {
		var batch = new Batch();
		batch.addItem(new Item("someItem"));
		batch.addItem(new Item("someOtherItem"));

		assertThat(batch.getItems()).hasSize(2);
		batch.removeItem(new Item("someItem"));
		assertThat(batch.getItems()).hasSize(1);
	}
}
