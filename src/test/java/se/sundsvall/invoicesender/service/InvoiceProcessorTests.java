package se.sundsvall.invoicesender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static se.sundsvall.invoicesender.TestDataFactory.createBatchEntity;
import static se.sundsvall.invoicesender.TestDataFactory.createItemEntity;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.IN_PROGRESS;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.SENT;
import static se.sundsvall.invoicesender.integration.db.entity.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.INVOICE;
import static se.sundsvall.invoicesender.integration.db.entity.ItemType.OTHER;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;
import se.sundsvall.invoicesender.integration.citizen.CitizenIntegration;
import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.entity.ItemEntity;
import se.sundsvall.invoicesender.integration.db.entity.ItemStatus;
import se.sundsvall.invoicesender.integration.db.entity.ItemType;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.integration.party.PartyIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegrationProperties;

@ExtendWith({
	MockitoExtension.class, ResourceLoaderExtension.class
})
class InvoiceProcessorTests {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private CitizenIntegration citizenIntegrationMock;

	@Mock
	private PartyIntegration partyIntegrationMock;

	@Mock
	private MessagingIntegration messagingIntegrationMock;

	@Mock
	private DbIntegration dbIntegrationMock;

	@Mock
	private RaindanceIntegrationProperties propertiesMock;

	@Mock
	private RaindanceIntegrationProperties.RaindanceEnvironment environmentMock;

	@InjectMocks
	private InvoiceProcessor invoiceProcessor;

	@BeforeAll
	static void setupStaticMock() {
		MockedStatic<FileUtils> fileUtilsMock = mockStatic(FileUtils.class);
		fileUtilsMock.when(() -> FileUtils.deleteDirectory(any())).thenAnswer(invocation -> null);
	}

	@BeforeEach
	void setup() {

		lenient().when(propertiesMock.environments()).thenReturn(Map.of(MUNICIPALITY_ID, environmentMock));
	}

	/**
	 * Test scenario where item is an invoice.
	 */
	@Test
	void markItems_1() {
		var invoiceFilenamePrefixes = Map.of("2281", List.of("faktura"));
		ReflectionTestUtils.setField(invoiceProcessor, "invoiceFilenamePrefixes", invoiceFilenamePrefixes);
		var itemEntity = createItemEntity(item -> item.setFilename("faktura123.pdf"));

		invoiceProcessor.markItems(itemEntity, MUNICIPALITY_ID);

		assertThat(itemEntity.getType()).isEqualTo(INVOICE);
		assertThat(itemEntity.getStatus()).isEqualTo(IN_PROGRESS);
	}

	/**
	 * Test scenario where item is an invoice but doesn't follow tha naming convention.
	 */
	@Test
	void markItems_2() {
		var invoiceFilenamePrefixes = Map.of(MUNICIPALITY_ID, List.of("faktura"));
		ReflectionTestUtils.setField(invoiceProcessor, "invoiceFilenamePrefixes", invoiceFilenamePrefixes);
		var itemEntity = createItemEntity(item -> item.setFilename("should-be-ignored.pdf"));

		invoiceProcessor.markItems(itemEntity, MUNICIPALITY_ID);

		assertThat(itemEntity.getType()).isEqualTo(INVOICE);
		assertThat(itemEntity.getStatus()).isEqualTo(IGNORED);
	}

	/**
	 * Test scenario where item is not an invoice.
	 */
	@Test
	void markItems_3() {
		var itemEntity = createItemEntity(item -> item.setFilename("should-be-ignored.jpeg"));

		invoiceProcessor.markItems(itemEntity, MUNICIPALITY_ID);

		assertThat(itemEntity.getType()).isEqualTo(OTHER);
		assertThat(itemEntity.getStatus()).isEqualTo(IGNORED);
	}

	/**
	 * Test scenario where item have complete metadata.
	 */
	@Test
	void extractItemMetadata_1(@Load("/files/ArchiveIndex.xml") final String xml) {
		var itemEntity = createItemEntity(item -> item.setFilename("Faktura_00000001_to_9001011234.pdf"));

		invoiceProcessor.extractItemMetadata(itemEntity, xml);

		assertThat(itemEntity.getMetadata()).satisfies(metadata -> {
			assertThat(metadata.getInvoiceNumber()).isEqualTo("123");
			assertThat(metadata.getInvoiceDate()).isEqualTo("2024-02-02");
			assertThat(metadata.getDueDate()).isEqualTo("2024-03-03");
			assertThat(metadata.isPayable()).isTrue();
			assertThat(metadata.isReminder()).isFalse();
			assertThat(metadata.getAccountNumber()).isEqualTo("1234-1234");
			assertThat(metadata.getPaymentReference()).isEqualTo("9001011234");
			assertThat(metadata.getTotalAmount()).isEqualTo("1000.00");
		});
		assertThat(itemEntity.getStatus()).isEqualTo(UNHANDLED);
	}

	/**
	 * Test scenario where item have incomplete metadata.
	 * <DueDate></DueDate> is missing for the given invoice.
	 */
	@Test
	void extractItemMetadata_2(@Load("/files/ArchiveIndex.xml") final String xml) {
		var itemEntity = createItemEntity(item -> item.setFilename("Faktura_00000002_to_9101011234.pdf"));

		invoiceProcessor.extractItemMetadata(itemEntity, xml);

		assertThat(itemEntity.getStatus()).isEqualTo(METADATA_INCOMPLETE);
	}

	@Test
	void validateLegalId() {
		var item1 = createItemEntity(item -> item.setRecipientLegalId("9001011234"));
		var item2 = createItemEntity(item -> item.setRecipientLegalId("202104142399"));

		invoiceProcessor.validateLegalId(item1);
		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		invoiceProcessor.validateLegalId(item2);
		assertThat(item2.getStatus()).isEqualTo(UNHANDLED);
	}

	/**
	 * Test scenario where recipient have protected identity.
	 */
	@Test
	void markProtectedIdentityItems_1() {
		var item1 = createItemEntity(item -> item.setRecipientLegalId("202104142399"));
		when(citizenIntegrationMock.hasProtectedIdentity(item1.getRecipientLegalId())).thenReturn(true);

		invoiceProcessor.markProtectedIdentityItems(item1);

		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
	}

	/**
	 * Test scenario where recipient does not have protected identity.
	 */
	@Test
	void markProtectedIdentityItems_2() {
		var item1 = createItemEntity(item -> item.setRecipientLegalId("202104142399"));
		when(citizenIntegrationMock.hasProtectedIdentity(item1.getRecipientLegalId())).thenReturn(false);

		invoiceProcessor.markProtectedIdentityItems(item1);

		assertThat(item1.getStatus()).isEqualTo(UNHANDLED);
	}

	/**
	 * Test scenario where recipient party id is found.
	 */
	@Test
	void fetchInvoiceRecipientPartyIds_1() {
		var item1 = createItemEntity(item -> item.setRecipientLegalId("202104142399"));
		when(partyIntegrationMock.getPartyId(item1.getRecipientLegalId(), MUNICIPALITY_ID)).thenReturn(Optional.of("1234"));

		invoiceProcessor.fetchInvoiceRecipientPartyIds(item1, MUNICIPALITY_ID);

		assertThat(item1.getRecipientPartyId()).isEqualTo("1234");
		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_PARTY_ID_FOUND);
	}

	/**
	 * Test scenario where recipient party id is not found.
	 */
	@Test
	void fetchInvoiceRecipientPartyIds_2() {
		var item1 = createItemEntity(item -> item.setRecipientLegalId("202104142399"));
		when(partyIntegrationMock.getPartyId(item1.getRecipientLegalId(), MUNICIPALITY_ID)).thenReturn(Optional.empty());

		invoiceProcessor.fetchInvoiceRecipientPartyIds(item1, MUNICIPALITY_ID);

		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_PARTY_ID_NOT_FOUND);
	}

	/**
	 * Test scenario where invoice is sent.
	 */
	@Test
	void sendDigitalInvoices_1() {
		var item1 = createItemEntity(item -> item.setRecipientPartyId("1234"));
		var localPath = "any/path/";
		when(messagingIntegrationMock.sendInvoice(localPath, item1, MUNICIPALITY_ID)).thenReturn(SENT);

		invoiceProcessor.sendDigitalInvoices(item1, localPath, MUNICIPALITY_ID);

		assertThat(item1.getStatus()).isEqualTo(SENT);
	}

	/**
	 * Test scenario where invoice is not sent.
	 */
	@Test
	void sendDigitalInvoices_2() {
		var item1 = createItemEntity(item -> item.setRecipientPartyId("1234"));
		var localPath = "any/path/";
		when(messagingIntegrationMock.sendInvoice(localPath, item1, MUNICIPALITY_ID)).thenReturn(NOT_SENT);

		invoiceProcessor.sendDigitalInvoices(item1, localPath, MUNICIPALITY_ID);

		assertThat(item1.getStatus()).isEqualTo(NOT_SENT);
	}

	@Test
	void extractInvoiceRecipientLegalId_1() {
		var item1 = createItemEntity(item -> item.setFilename("Faktura_00000001_to_9001011234.pdf"));

		invoiceProcessor.extractInvoiceRecipientLegalId(item1);

		assertThat(item1.getRecipientLegalId()).isEqualTo("9001011234");
		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_FOUND);
	}

	@Test
	void extractInvoiceRecipientLegalId_2() {
		var item1 = createItemEntity(item -> item.setFilename("Faktura_00000001_to_.pdf"));

		invoiceProcessor.extractInvoiceRecipientLegalId(item1);

		assertThat(item1.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
	}

	@Test
	void updateAndPersistBatch() {
		var batch = createBatchEntity();
		var items = List.of(
			createItemEntity(item -> item.setStatus(SENT)),
			createItemEntity(item -> item.setStatus(IGNORED)));
		batch.setItems(items);
		doNothing().when(dbIntegrationMock).persistBatch(batch);

		invoiceProcessor.updateAndPersistBatch(batch);

		assertThat(batch.isCompleted()).isTrue();
		assertThat(batch.getCompletedAt()).isCloseTo(LocalDateTime.now(), within(5, ChronoUnit.SECONDS));
		assertThat(batch.getIgnoredItems()).isEqualTo(1);
		assertThat(batch.getSentItems()).isEqualTo(1);

		verify(dbIntegrationMock).persistBatch(batch);
	}

	/**
	 * Test the scenario where the invoice does not pass the markItems check
	 */
	@Test
	void run_1() throws IOException {
		var item = createItemEntity();
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(IGNORED)).when(spy).markItems(item, MUNICIPALITY_ID);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy, never()).extractItemMetadata(any(), any());
		verify(spy, never()).extractInvoiceRecipientLegalId(any());
		verify(spy, never()).validateLegalId(any());
		verify(spy, never()).markProtectedIdentityItems(any());
		verify(spy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the extractItemMetadata check
	 */
	@Test
	void run_2() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(updateItem(METADATA_INCOMPLETE)).when(spy).extractItemMetadata(item, "mocked-string");

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy, never()).extractInvoiceRecipientLegalId(any());
		verify(spy, never()).validateLegalId(any());
		verify(spy, never()).markProtectedIdentityItems(any());
		verify(spy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the extractInvoiceRecipientLegalId check
	 */
	@Test
	void run_3() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(spy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(spy).extractInvoiceRecipientLegalId(item);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy).extractInvoiceRecipientLegalId(item);
		verify(spy, never()).validateLegalId(any());
		verify(spy, never()).markProtectedIdentityItems(any());
		verify(spy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the validateLegalId check
	 */
	@Test
	void run_4() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(spy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(spy).extractInvoiceRecipientLegalId(item);
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(spy).validateLegalId(item);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy).extractInvoiceRecipientLegalId(item);
		verify(spy).validateLegalId(item);
		verify(spy, never()).markProtectedIdentityItems(any());
		verify(spy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the markProtectedIdentityItems check
	 */
	@Test
	void run_5() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(spy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(spy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(spy).validateLegalId(item);
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(spy).markProtectedIdentityItems(item);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy).extractInvoiceRecipientLegalId(item);
		verify(spy).validateLegalId(item);
		verify(spy).markProtectedIdentityItems(item);
		verify(spy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the fetchInvoiceRecipientPartyIds check
	 */
	@Test
	void run_6() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(spy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(spy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(spy).validateLegalId(item);
		doAnswer(doNotUpdate()).when(spy).markProtectedIdentityItems(item);
		doAnswer(updateItem(RECIPIENT_PARTY_ID_NOT_FOUND)).when(spy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy).extractInvoiceRecipientLegalId(item);
		verify(spy).validateLegalId(item);
		verify(spy).markProtectedIdentityItems(item);
		verify(spy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		verify(spy, never()).sendDigitalInvoices(any(), any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the sendDigitalInvoices check
	 */
	@Test
	void run_7() throws IOException {
		var item = createItemEntity(item1 -> item1.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var spy = Mockito.spy(invoiceProcessor);
		runMethodCommonStubs(item, spy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(spy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(spy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(spy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(spy).validateLegalId(item);
		doAnswer(doNotUpdate()).when(spy).markProtectedIdentityItems(item);
		doAnswer(updateItem(RECIPIENT_PARTY_ID_FOUND)).when(spy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		doAnswer(updateItem(NOT_SENT)).when(spy).sendDigitalInvoices(item, "mocked-path", MUNICIPALITY_ID);

		spy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(spy).markItems(item, MUNICIPALITY_ID);
		verify(spy).extractItemMetadata(item, "mocked-string");
		verify(spy).extractInvoiceRecipientLegalId(item);
		verify(spy).validateLegalId(item);
		verify(spy).markProtectedIdentityItems(item);
		verify(spy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		verify(spy).sendDigitalInvoices(item, "mocked-path", MUNICIPALITY_ID);
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Some methods update the ItemType and ItemStatus of the item
	 */
	private Answer<ItemEntity> updateItem(ItemType type, ItemStatus status) {
		return invocation -> {
			var invocationArgument = (ItemEntity) invocation.getArgument(0);
			Optional.ofNullable(type).ifPresent(invocationArgument::setType);
			Optional.ofNullable(status).ifPresent(invocationArgument::setStatus);
			return null;
		};
	}

	/**
	 * Some methods only updates the status of the item.
	 */
	private Answer<ItemEntity> updateItem(ItemStatus status) {
		return updateItem(null, status);
	}

	/**
	 * Some methods do not update the item if it passes the checks.
	 */
	private Answer<ItemEntity> doNotUpdate() {
		return invocation -> null;
	}

	/**
	 * Common stubs for the run(LocalDate, String, String) method tests
	 */
	private void runMethodCommonStubs(final ItemEntity item, final InvoiceProcessor invoiceProcessor) throws IOException {
		var date = LocalDate.now();
		var batch = createBatchEntity(batch1 -> batch1.setLocalPath("mocked-path")).withItems(List.of(item));
		var batches = List.of(batch);

		var raindanceIntegration = mock(RaindanceIntegration.class);
		var raindanceIntegrations = Map.of(MUNICIPALITY_ID, raindanceIntegration);
		ReflectionTestUtils.setField(invoiceProcessor, "raindanceIntegrations", raindanceIntegrations);

		when(raindanceIntegration.readBatches(date, "BatchName", "2281")).thenReturn(batches);
		doReturn("mocked-string").when(invoiceProcessor).mapXmlFileToString(anyString());
		when(dbIntegrationMock.persistBatches(batches)).thenReturn(batches);
		doNothing().when(raindanceIntegration).writeBatch(batch);
		doNothing().when(raindanceIntegration).archiveOriginalBatch(batch);
		doNothing().when(invoiceProcessor).updateAndPersistBatch(batch);
		lenient().doReturn("mocked-string").when(invoiceProcessor).removeItemFromArchiveIndex(item, "mocked-string", "mocked-path");
		doNothing().when(messagingIntegrationMock).sendStatusReport(batches, MUNICIPALITY_ID);
	}

}
