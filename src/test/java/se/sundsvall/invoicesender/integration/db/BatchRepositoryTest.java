package se.sundsvall.invoicesender.integration.db;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.integration.db.entity.BatchStatus.MANAGED;
import static se.sundsvall.invoicesender.integration.db.entity.BatchStatus.READY;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/scripts/truncate.sql",
	"/db/scripts/testdata.sql"
})
class BatchRepositoryTest {
	@Autowired
	private BatchRepository batchRepositoryMock;

	@Test
	void testFindAllByBatchStatusReady() {
		var result = batchRepositoryMock.findAllByBatchStatus(READY);

		assertThat(result).hasSize(2);

		assertThat(result).allSatisfy(batch -> assertThat(batch.getBatchStatus()).isEqualTo(READY));
	}

	@Test
	void testFindAllByBatchStatusManaged() {
		var result = batchRepositoryMock.findAllByBatchStatus(MANAGED);

		assertThat(result).hasSize(2);
		assertThat(result).allSatisfy(batch -> assertThat(batch.getBatchStatus()).isEqualTo(MANAGED));
	}

}
