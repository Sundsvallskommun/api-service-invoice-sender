package apptest;

import jcifs.CIFSContext;
import jcifs.smb.SmbFile;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.invoicesender.Application;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.service.InvoiceProcessor;
import se.sundsvall.invoicesender.service.util.XmlUtil;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static apptest.util.TestUtil.extractZipFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static se.sundsvall.invoicesender.util.Constants.X_PATH_FILENAME_EXPRESSION;

@Testcontainers
@WireMockAppTestSuite(files = "classpath:/InvoiceSenderIT/", classes = Application.class)
class InvoiceSenderIT extends AbstractAppTest {

	private static final String ARCHIVE_PATH = "smb://localhost:1445/files/archive/";
	private static final String RETURN_PATH = "smb://localhost:1445/files/return/";
	private static final String MUNICIPALITY_ID = "2281";
	private static final String SERVICE_PATH = "/2281/batches/trigger";
	private static final String BASE_DIR = "src/integration-test/resources";
	private static final String SHARE_DIR = BASE_DIR + "/raindance-share";
	private static final String ARCHIVE_INDEX_XML = "ArchiveIndex.xml";

	@Autowired
	private InvoiceProcessor invoiceProcessor;

	@Container
	public static final DockerComposeContainer<?> sambaContainer =
		new DockerComposeContainer<>(new File(BASE_DIR + "/docker-compose.yml"))
			.withStartupTimeout(Duration.ofSeconds(60));

	@AfterAll
	static void cleanUp() throws IOException {
		FileUtils.cleanDirectory(new File(SHARE_DIR));
	}

	/**
	 * Tests the scenario where no invoices are sent.
	 */
	@Test
	void test1_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200101_000001.zip.7z";
		FileUtils.copyFileToDirectory(new File(BASE_DIR + File.separator + inputFile), new File(SHARE_DIR));

		// Invoices that are part of the ZIP file and the status that we expect.
		List<Invoice> invoices = List.of(
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000001_to_9001011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000002_to_9101011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000003_to_9201011234.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_9301011234.pdf"));

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-01")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RETURN_PATH + inputFile, getCIFSContext())) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(ARCHIVE_PATH + inputFile, getCIFSContext())) {
			var originalFile = new File(BASE_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	/**
	 * Tests the scenario where some invoices are sent and some are not.
	 */
	@Test
	void test2_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200102_000002.zip.7z";
		FileUtils.copyFileToDirectory(new File(BASE_DIR + File.separator + inputFile), new File(SHARE_DIR));

		// Invoices that are part of the ZIP file and the status that we expect.
		List<Invoice> invoices = List.of(
			new Invoice(Invoice.Status.SENT, "Faktura_00000001_to_202107142388.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000002_to_202108022399.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000003_to_202108132388.pdf"),
			new Invoice(Invoice.Status.NOT_SENT, "Faktura_00000004_to_20323217.pdf"));

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-02")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RETURN_PATH + inputFile, getCIFSContext())) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(ARCHIVE_PATH + inputFile, getCIFSContext())) {
			var originalFile = new File(BASE_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	/**
	 * Tests the scenario where all invoices are sent.
	 */
	@Test
	void test3_processInvoices() throws IOException {
		var inputFile = "Faktura-pdf-200103_000003.zip.7z";
		FileUtils.copyFileToDirectory(new File(BASE_DIR + File.separator + inputFile), new File(SHARE_DIR));

		// Invoices that are part of the ZIP file and the status that we expect.
		List<Invoice> invoices = List.of(
			new Invoice(Invoice.Status.SENT, "Faktura_00000001_to_202107142388.pdf"),
			new Invoice(Invoice.Status.SENT, "Faktura_00000002_to_202108022399.pdf"));

		setupCall()
			.withServicePath(SERVICE_PATH + "/2020-01-03")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(OK)
			.sendRequestAndVerifyResponse();

		// Asserts that the ZIP in the return folder contains the expected entries
		try (var outFile = new SmbFile(RETURN_PATH + inputFile, getCIFSContext())) {
			assertThat(outFile.exists()).isTrue();
			assertThat(outFile.isFile()).isTrue();
			assertArchiveIndex(invoices, outFile);
			assertZipEntries(invoices, outFile);
			assertZipOnlyContainsArchiveIndex(outFile);
		}

		// Asserts that the original ZIP is equal to the ZIP in the archive folder
		try (var archiveFile = new SmbFile(ARCHIVE_PATH + inputFile, getCIFSContext())) {
			var originalFile = new File(BASE_DIR + File.separator + inputFile);
			assertThat(extractZipFile(archiveFile)).usingRecursiveComparison().isEqualTo(extractZipFile(originalFile));
		}
	}

	private void assertArchiveIndex(final List<Invoice> invoices, final SmbFile outFile) throws IOException {
		var zipEntries = extractZipFile(outFile);
		var archiveIndex = zipEntries.get(ARCHIVE_INDEX_XML);
		assertThat(archiveIndex).isNotNull();

		var archiveIndexXml = new String(zipEntries.get(ARCHIVE_INDEX_XML));

		// Ensure that the ArchiveIndex.xml contains entries for all invoices that weren't sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.NOT_SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isNotEmpty());

		//Ensure that the ArchiveIndex.xml does not contain entries for invoices that were sent
		invoices.stream()
			.filter(invoice -> invoice.status() == Invoice.Status.SENT)
			.map(invoice -> XmlUtil.find(archiveIndexXml, X_PATH_FILENAME_EXPRESSION.formatted(invoice.filename())))
			.forEach(elements -> assertThat(elements).isEmpty());

	}

	private void assertZipOnlyContainsArchiveIndex(final SmbFile outFile) throws IOException {
		var zipEntries = extractZipFile(outFile);
		assertThat(zipEntries).containsOnlyKeys(ARCHIVE_INDEX_XML);
	}

	private void assertZipEntries(final List<Invoice> invoices, final SmbFile outFile) throws IOException {
		var zipEntries = extractZipFile(outFile);

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
		private enum Status {SENT, NOT_SENT}
	}

	private CIFSContext getCIFSContext() {
		var raindanceIntegrations = (Map<String, RaindanceIntegration>) ReflectionTestUtils.getField(invoiceProcessor, "raindanceIntegrations");
		var raindanceIntegration = raindanceIntegrations.get(InvoiceSenderIT.MUNICIPALITY_ID);
		return (CIFSContext) ReflectionTestUtils.getField(raindanceIntegration, "context");
	}

}
