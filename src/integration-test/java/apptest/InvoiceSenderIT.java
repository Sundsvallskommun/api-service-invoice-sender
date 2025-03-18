package apptest;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.invoicesender.util.Constants.X_PATH_FILENAME_EXPRESSION;
import static se.sundsvall.invoicesender.util.IOUtil.copy;
import static se.sundsvall.invoicesender.util.IOUtil.decompressLzma;
import static se.sundsvall.invoicesender.util.IOUtil.unzip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import jcifs.CIFSContext;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegrationProperties;
import se.sundsvall.invoicesender.service.util.XmlUtil;

@Testcontainers
@WireMockAppTestSuite(files = "classpath:/InvoiceSenderIT/", classes = Application.class)
class InvoiceSenderIT extends AbstractAppTest {

	private static final String SERVICE_PATH = "/2281/batches/trigger";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String ARCHIVE_INDEX_XML = "ArchiveIndex.xml";

	private static final String BASE_DIR = "src/integration-test/resources";
	private static final String TESTDATA_DIR = BASE_DIR + "/testdata";

	private static final String RAINDANCE_ARCHIVE_DIR = "smb://localhost:%d/files/archive/%s";
	private static final String RAINDANCE_RETURN_DIR = "smb://localhost:%d/files/return/%s";

	@Autowired
	private RaindanceIntegrationProperties raindanceIntegrationProperties;

	private static int smbContainerPort;
	private static CIFSContext cifsContext;

	@Container
	public static GenericContainer<?> smbContainer = new GenericContainer<>("dockurr/samba")
		.withExposedPorts(445)
		.withEnv(Map.of(
			"NAME", "files",
			"USER", "user",
			"PASS", "p4ssw0rd"
		))
		.withTmpFs(Map.of("/storage/return", "rw", "/storage/archive", "rw"))
		.withClasspathResourceMapping("testdata", "/storage/incoming", BindMode.READ_WRITE);

	@DynamicPropertySource
	static void afterSambaContainerStarted(final DynamicPropertyRegistry registry) {
		smbContainerPort = smbContainer.getMappedPort(445);

		registry.add("samba.port", () -> smbContainerPort);
	}

	@BeforeEach
	void setUp() throws Exception {
		// Initialize the JCIFS context, if needed
		if (cifsContext == null) {
			var raindanceEnvironment = raindanceIntegrationProperties.environments().get(MUNICIPALITY_ID);
			var config = new PropertyConfiguration(raindanceEnvironment.jcifsProperties());
			cifsContext = new BaseContext(config)
				.withCredentials(new NtlmPasswordAuthenticator(
					raindanceEnvironment.domain(), raindanceEnvironment.username(), raindanceEnvironment.password()));
		}
	}

	/**
	 * Tests the scenario where no invoices are sent.
	 */
	@Test
	void test1_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200101_000001.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-01")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Invoices that are part of the ZIP file and the status that we expect.
		var invoices = List.of(
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000001_to_9001011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000002_to_9101011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000003_to_9201011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_9301011234.pdf"));

		// Assert that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();

			assertThatArchiveIndexExistsAndContainsInvoices(outFile, invoices);
		}

		// Assert that the original ZIP is equal to the ZIP in the archive folder
		assertThatArchivedFileIsEqualToOriginalFile(inputFile);
	}

	/**
	 * Tests the scenario where some invoices are sent and some are not.
	 */
	@Test
	void test2_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200102_000002.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-02")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Invoices that are part of the ZIP file and the status that we expect.
		var invoices = List.of(
			new Invoice(Invoice.Status.SENT, "Faktura_00000001_to_202107142388.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000002_to_202108022399.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000003_to_202108132388.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_20323217.pdf"));

		// Assert that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();

			assertThatArchiveIndexExistsAndContainsInvoices(outFile, invoices);
		}

		// Assert that the original ZIP is equal to the ZIP in the archive folder
		assertThatArchivedFileIsEqualToOriginalFile(inputFile);
	}

	/**
	 * Tests the scenario where all invoices are sent.
	 */
	@Test
	void test3_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200103_000003.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-03")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Assert that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();

			assertThatArchiveIndexExists(outFile);
		}

		// Assert that the original ZIP is equal to the ZIP in the archive folder
		assertThatArchivedFileIsEqualToOriginalFile(inputFile);
	}

	private void assertThatArchiveIndexExists(final SmbFile smbFile) throws IOException {
		assertThatArchiveIndexExistsAndContainsInvoices(smbFile, List.of());
	}

	private void assertThatArchiveIndexExistsAndContainsInvoices(final SmbFile smbFile, final List<Invoice> invoices) throws IOException {
		var decompressedLzmaData = decompressLzma(smbFile.getInputStream());
		var zipEntries = unzip(new ByteArrayInputStream(decompressedLzmaData));

		assertThat(zipEntries).containsKey(ARCHIVE_INDEX_XML);

		var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML), ISO_8859_1);

		// Ensure that the ArchiveIndex.xml contains entries for all invoices that weren't sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.NOT_SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isNotEmpty());

		// Ensure that the ArchiveIndex.xml does not contain entries for invoices that were sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isEmpty());
	}

	private void assertThatArchivedFileIsEqualToOriginalFile(final String inputFile) throws IOException {
		try (var archiveFile = new SmbFile(RAINDANCE_ARCHIVE_DIR.formatted(smbContainerPort, inputFile), cifsContext);
			var archiveFileByteArrayOutputStream = new ByteArrayOutputStream()) {

			copy(archiveFile.getInputStream(), archiveFileByteArrayOutputStream);
			var archiveFileBytes = archiveFileByteArrayOutputStream.toByteArray();
			var originalFileBytes = Files.readAllBytes(Paths.get(TESTDATA_DIR + File.separator + inputFile));

			assertThat(archiveFileBytes).isEqualTo(originalFileBytes);
		}
	}

	private record Invoice(Status status, String filename) {
		private enum Status {SENT, NOT_SENT}
	}
}
