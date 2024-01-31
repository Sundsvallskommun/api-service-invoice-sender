package apptest;

import static apptest.util.TestUtil.extractZipFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.service.util.XmlUtil;

@Testcontainers
@WireMockAppTestSuite(files = "classpath:/InvoiceSenderIT/", classes = Application.class)
class InvoiceSenderIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/batches/trigger";

	private static final String BASE_DIR = "/tmp/invoice-sender-integration-test";
	private static final String INPUT_FILE = "Faktura-pdf-241222_120505.zip.7z";
	private static final String SHARE_DIR = BASE_DIR + "/raindance-share";

	private static final String ARCHIVE_INDEX_XML = "ArchiveIndex.xml";

	static DockerComposeContainer<?> samba;

	/*
	 * Start the Samba container manually instead of having Testcontainers manage its lifecycle,
	 * since we need to load the docker-compose file from the classpath at the same time as we
	 * access filesystem resources mounted as volumes in the container.
	 */
	@BeforeAll
	static void beforeAll() throws IOException {
		samba = new DockerComposeContainer<>(new ClassPathResource("docker-compose.yml").getFile())
			.withExposedService("samba", 445, Wait.forListeningPort())
			.withStartupTimeout(Duration.ofSeconds(60));
		samba.start();

		FileUtils.forceMkdir(new File(BASE_DIR));
	}

	/*
	 * Stop the Samba container manually when all tests have executed.
	 */
	@AfterAll
	static void afterAll() throws IOException {
		samba.stop();

		FileUtils.deleteDirectory(new File(BASE_DIR));
	}

	/*
	 * Copy the fake invoice ZIP file to the share directory.
	 */
	@BeforeEach
	void beforeEach() throws Exception {
		FileUtils.copyFileToDirectory(new ClassPathResource(INPUT_FILE).getFile(), new File(SHARE_DIR));
	}

	/*
	 * Remove everything in the share directory.
	 */
	@AfterEach
	void afterEach() throws IOException {
		FileUtils.cleanDirectory(new File(SHARE_DIR));
	}

	@Test
	void test1_allOk() throws Exception {
		setupCall()
			.withServicePath(SERVICE_PATH + "/2024-12-22")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		var outFile = new File(SHARE_DIR + File.separator + INPUT_FILE);
		assertThat(outFile).exists().isFile();

		// Extract
		var zipEntries = extractZipFile(outFile);

		// Verify
		assertThat(zipEntries).hasSize(1).containsOnlyKeys(ARCHIVE_INDEX_XML);
		assertThat(zipEntries.get(ARCHIVE_INDEX_XML)).satisfies(zipEntryData -> {
			// Ensure the XML contains no entries (<file> nodes)
			var archiveIndexXml = new String(zipEntryData);

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

		var outFile = new File(SHARE_DIR + File.separator + INPUT_FILE);
		assertThat(outFile).exists().isFile();

		// Extract
		var zipEntries = extractZipFile(outFile);

		// Verify
		var archiveIndex = zipEntries.get(ARCHIVE_INDEX_XML);
		assertThat(archiveIndex).isNotNull();

		// Ensure the archive index XML contains a single entry (<file> node)
		var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML));
		var fileNodes = XmlUtil.find(archiveIndexXml, "//index/file");
		assertThat(fileNodes).hasSize(1);

		// Extract the filename of the PDF/invoice that wasn't sent and make sure it exists in the
		// ZIP file (entries)
		var filename = fileNodes.getFirst().selectXpath("./filename").text();
		assertThat(filename).isNotBlank();
		assertThat(zipEntries).containsOnlyKeys(ARCHIVE_INDEX_XML, filename);
	}
}
