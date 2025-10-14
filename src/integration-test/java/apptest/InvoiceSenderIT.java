package apptest;

import static apptest.util.TestUtil.extractZipFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.invoicesender.util.Constants.X_PATH_FILENAME_EXPRESSION;

import java.io.File;
import java.io.IOException;
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

	private static final String RAINDANCE_INCOMING_DIR = "smb://localhost:%d/files/incoming/%s";
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
			"PASS", "p4ssw0rd"))
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
			final var raindanceEnvironment = raindanceIntegrationProperties.environments().get(MUNICIPALITY_ID);
			final var config = new PropertyConfiguration(raindanceEnvironment.jcifsProperties());
			cifsContext = new BaseContext(config)
				.withCredentials(new NtlmPasswordAuthenticator(
					raindanceEnvironment.domain(), raindanceEnvironment.username(), raindanceEnvironment.password()));
		}
	}

	/**
	 * Tests the scenario where no invoices are sent.
	 */
	@Test
	void test1_processInvoicesAllNotSent() throws IOException {
		final var inputFile = "Faktura-pdf-200101_000001.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-01")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Invoices that are part of the ZIP file and the status that we expect.
		final var invoices = List.of(
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000001_to_9001011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000002_to_9101011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000003_to_9201011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_9301011234.pdf"));

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(RAINDANCE_ARCHIVE_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			final var originalFile = new File(TESTDATA_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	/**
	 * Tests the scenario where some invoices are sent and some are not.
	 */
	@Test
	void test2_processInvoicesSomeNotSent() throws IOException {
		final var inputFile = "Faktura-pdf-200102_000002.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-02")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Invoices that are part of the ZIP file and the status that we expect.
		final var invoices = List.of(
			new Invoice(Invoice.Status.SENT, "Faktura_00000001_to_202107142388.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000002_to_202108022399.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000003_to_202108132388.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_20323217.pdf"));

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(RAINDANCE_ARCHIVE_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			final var originalFile = new File(TESTDATA_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	/**
	 * Tests the scenario where all invoices are sent.
	 */
	@Test
	void test3_processInvoicesAllSent() throws IOException {
		final var inputFile = "Faktura-pdf-200103_000003.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-03")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Invoices that are part of the ZIP file and the status that we expect.
		final var invoices = List.of(
			new Invoice(Invoice.Status.SENT, "Faktura_00000001_to_202107142388.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000002_to_202108022399.pdf"));

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
			assertZipOnlyContainsArchiveIndex(outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(RAINDANCE_ARCHIVE_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			final var originalFile = new File(TESTDATA_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	/**
	 * Tests the scenario where invalid certificate is thrown
	 */
	@Test
	void test4_processInvoicesWhileCertificateProblem() throws IOException {
		final var inputFile = "Faktura-pdf-200104_000004.zip.7z";

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-04")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Asserts that there is no ZIP in the return folder
		try (var outFile = new SmbFile(RAINDANCE_RETURN_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(outFile.exists()).isFalse(); // No return file should exist as execution has not processed file due to certificate problem
		}
		// Asserts that there is no ZIP in the archive folder
		try (var archiveFile = new SmbFile(RAINDANCE_ARCHIVE_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			assertThat(archiveFile.exists()).isFalse(); // No archive file should exist as execution has not processed file due to certificate problem
		}

		// Asserts that the original ZIP is untouched
		try (var rootFile = new SmbFile(RAINDANCE_INCOMING_DIR.formatted(smbContainerPort, inputFile), cifsContext)) {
			// Invoices that are part of the ZIP file and should be there
			final var expectedInvoiceEntries = List.of(
				"Faktura_00000001_to_2107142388.pdf",
				"Faktura_00000002_to_2108022399.pdf");

			assertOriginalFile(rootFile, expectedInvoiceEntries);
		}
	}

	private void assertOriginalFile(SmbFile rootFile, final List<String> expectedInvoiceEntries) throws IOException {
		final var zipEntries = extractZipFile(rootFile);
		final var archiveIndex = zipEntries.get(ARCHIVE_INDEX_XML);
		final var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML));

		assertThat(archiveIndex).isNotNull();
		expectedInvoiceEntries.stream()
			.map(filename -> {
				// Ensures that the root file still contains entries for all invoices (has not been tampered with)
				assertThat(zipEntries).containsKey(filename);
				return filename;
			})
			.map(filename -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(filename)))
			.forEach(elements -> {
				// Ensure that the ArchiveIndex.xml in root file contains entries for all invoices (is not tampered with)
				assertThat(elements).isNotNull();
			});
	}

	private void assertArchiveIndex(final List<Invoice> invoices, final SmbFile outFile) throws IOException {
		final var zipEntries = extractZipFile(outFile);
		final var archiveIndex = zipEntries.get(ARCHIVE_INDEX_XML);
		assertThat(archiveIndex).isNotNull();

		final var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML));

		// Ensure that the ArchiveIndex.xml contains entries for all invoices that weren't sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.NOT_SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isNotNull());

		// Ensure that the ArchiveIndex.xml does not contain entries for invoices that were sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isNull());
	}

	private void assertZipOnlyContainsArchiveIndex(final SmbFile outFile) throws IOException {
		final var zipEntries = extractZipFile(outFile);
		assertThat(zipEntries).containsOnlyKeys(ARCHIVE_INDEX_XML);
	}

	private void assertZipEntries(final List<Invoice> invoices, final SmbFile outFile) throws IOException {
		final var zipEntries = extractZipFile(outFile);

		// Ensures that the ZIP file contains entries for all invoices that weren't sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.NOT_SENT)
			.map(Invoice::filename)
			.forEach(filename -> assertThat(zipEntries).containsKey(filename));

		// Ensures that the ZIP file does not contain entries for invoices that were sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.SENT)
			.map(Invoice::filename)
			.forEach(filename -> assertThat(zipEntries).doesNotContainKey(filename));
	}

	private record Invoice(Status status, String filename) {
		private enum Status {
			SENT, NOT_SENT
		}
	}
}
