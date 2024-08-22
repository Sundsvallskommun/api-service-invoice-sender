package apptest;

import static apptest.util.TestUtil.extractZipFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.service.util.XmlUtil;

@Disabled
@Testcontainers
@WireMockAppTestSuite(files = "classpath:/InvoiceSenderIT/", classes = Application.class)
class InvoiceSenderIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/2281/batches/trigger";

	private static final String BASE_DIR = "src/integration-test/resources";

	@Container
	static final DockerComposeContainer<?> sambaContainer =
		new DockerComposeContainer<>(new File(BASE_DIR + "/docker-compose.yml"))
			.withExposedService("samba", 445, Wait.forListeningPort())
			.withStartupTimeout(Duration.ofSeconds(60));

	private static final String INPUT_FILE = "Faktura-pdf-241222_120505.zip.7z";

	private static final String SHARE_DIR = BASE_DIR + "/raindance-share";

	private static final String ARCHIVE_INDEX_XML = "ArchiveIndex.xml";

	@BeforeEach
	void setUp() throws IOException {
		FileUtils.copyFileToDirectory(new File(BASE_DIR + File.separator + INPUT_FILE), new File(SHARE_DIR));
	}

	@AfterEach
	void cleanUp() throws IOException {
		FileUtils.cleanDirectory(new File(SHARE_DIR));
	}

	@Test
	void test1_allOk() throws Exception {
		setupCall()
			.withServicePath(SERVICE_PATH + "/2024-12-22")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		final var outFile = new File(SHARE_DIR + File.separator + INPUT_FILE);
		assertThat(outFile).exists().isFile();

		// Extract
		final var zipEntries = extractZipFile(outFile);

		// Verify
		assertThat(zipEntries).hasSize(1).containsOnlyKeys(ARCHIVE_INDEX_XML);
		assertThat(zipEntries.get(ARCHIVE_INDEX_XML)).satisfies(zipEntryData -> {
			// Ensure the XML contains no entries (<file> nodes)
			final var archiveIndexXml = new String(zipEntryData);

			assertThat(XmlUtil.find(archiveIndexXml, "//index/file")).isEmpty();
		});
	}

	@Test
	void test2_notFoundFromParty() throws Exception {
		setupCall()
			.withServicePath(SERVICE_PATH + "/2024-12-22")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		final var outFile = new File(SHARE_DIR + File.separator + INPUT_FILE);
		assertThat(outFile).exists().isFile();

		// Extract
		final var zipEntries = extractZipFile(outFile);

		// Verify
		final var archiveIndex = zipEntries.get(ARCHIVE_INDEX_XML);
		assertThat(archiveIndex).isNotNull();

		// Ensure the archive index XML contains a single entry (<file> node)
		final var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML));
		final var fileNodes = XmlUtil.find(archiveIndexXml, "//index/file");
		assertThat(fileNodes).hasSize(1);

		// Extract the filename of the PDF/invoice that wasn't sent and make sure it exists in the
		// ZIP file (entries)
		final var filename = fileNodes.getFirst().selectXpath("./filename").text();
		assertThat(filename).isNotBlank();
		assertThat(zipEntries).containsOnlyKeys(ARCHIVE_INDEX_XML, filename);
	}

}
