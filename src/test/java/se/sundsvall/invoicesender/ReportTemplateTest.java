package se.sundsvall.invoicesender;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.invoicesender.TestDataFactory.createBatchEntity;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class ReportTemplateTest {

	@Autowired
	private SpringTemplateEngine templateEngine;

	@Test
	void testEmptyBatchesStatusReport() {
		final var context = new Context();
		context.setVariable("batches", emptyList());

		final var result = templateEngine.process("status-report", context);

		assertThat(result).contains("Inga filer har behandlats.");
	}

	@Test
	void testNonEmptyBatchesStatusReport() {
		final var batchEntity = createBatchEntity();
		batchEntity.setTotalItems(60L);
		batchEntity.setSentItems(30L);

		final var anotherBatchEntity = createBatchEntity();
		anotherBatchEntity.setTotalItems(40L);
		anotherBatchEntity.setSentItems(10L);

		final var context = new Context();
		context.setVariable("batches", List.of(batchEntity, anotherBatchEntity));

		final var result = templateEngine.process("status-report", context);

		// First batch
		assertThat(result)
			.containsPattern("<td>Totalt antal fakturor:</td>\\s*<td>59</td>")
			.containsPattern("<td>Antal skickade som digital post:</td>\\s*<td>30</td>")
			.containsPattern("<td>Antal ej behandlade:</td>\\s*<td>29</td>")
			// Second batch
			.containsPattern("<td>Totalt antal fakturor:</td>\\s*<td>39</td>")
			.containsPattern("<td>Antal skickade som digital post:</td>\\s*<td>10</td>")
			.containsPattern("<td>Antal ej behandlade:</td>\\s*<td>29</td>");
	}

	@Test
	void testErrorReport() {
		final var requestId = UUID.randomUUID().toString();
		final var municipalityId = "2281";
		final var batchName = "SomeBatchName";
		final var message = "Some error message";

		final var context = new Context();
		context.setVariable("requestId", requestId);
		context.setVariable("municipalityId", municipalityId);
		context.setVariable("batchName", batchName);
		context.setVariable("message", message);

		final var result = templateEngine.process("error-report", context);

		assertThat(result).containsPattern("<p>\\s*Ett kritiskt fel inträffade vid exekvering av batch <span>SomeBatchName</span> för kommunkod <span>" + municipalityId + "</span>\\s*</p>")
			.containsPattern("<tr>\\s*<td class=\"spacer\">\\s*Felmeddelande:\\s*</td>\\s*</tr>\\s*<tr>\\s*<td>\\s*<code>Some error message</code>\\s*<td>\\s*</tr>")
			.containsPattern("<tr>\\s*<td class=\"spacer\">\\s*Filtrera loggar i ELK på requestId <b><span>" + requestId + "</span></b> för ytterligare information kring felet\\.\\s*</td>\\s*</tr>");
	}
}
