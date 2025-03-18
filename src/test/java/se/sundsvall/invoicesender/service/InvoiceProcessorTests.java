package se.sundsvall.invoicesender.service;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import se.sundsvall.invoicesender.integration.party.LegalIdAndPartyId;
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
		var fileUtilsMock = mockStatic(FileUtils.class);
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
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("faktura123.pdf"));

		invoiceProcessor.markItems(item, MUNICIPALITY_ID);

		assertThat(item.getType()).isEqualTo(INVOICE);
		assertThat(item.getStatus()).isEqualTo(IN_PROGRESS);
	}

	/**
	 * Test scenario where item is an invoice but doesn't follow tha naming convention.
	 */
	@Test
	void markItems_2() {
		var invoiceFilenamePrefixes = Map.of(MUNICIPALITY_ID, List.of("faktura"));
		ReflectionTestUtils.setField(invoiceProcessor, "invoiceFilenamePrefixes", invoiceFilenamePrefixes);
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("should-be-ignored.pdf"));

		invoiceProcessor.markItems(item, MUNICIPALITY_ID);

		assertThat(item.getType()).isEqualTo(INVOICE);
		assertThat(item.getStatus()).isEqualTo(IGNORED);
	}

	/**
	 * Test scenario where item is not an invoice.
	 */
	@Test
	void markItems_3() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("should-be-ignored.jpeg"));

		invoiceProcessor.markItems(item, MUNICIPALITY_ID);

		assertThat(item.getType()).isEqualTo(OTHER);
		assertThat(item.getStatus()).isEqualTo(IGNORED);
	}

	/**
	 * Test scenario where item have complete metadata.
	 */
	@Test
	void extractItemMetadata_1(@Load("/files/ArchiveIndex.xml") final String xml) {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));

		invoiceProcessor.extractItemMetadata(item, xml);

		assertThat(item.getMetadata()).satisfies(metadata -> {
			assertThat(metadata.getInvoiceNumber()).isEqualTo("123");
			assertThat(metadata.getInvoiceDate()).isEqualTo("2024-02-02");
			assertThat(metadata.getDueDate()).isEqualTo("2024-03-03");
			assertThat(metadata.isPayable()).isTrue();
			assertThat(metadata.isReminder()).isFalse();
			assertThat(metadata.getAccountNumber()).isEqualTo("1234-1234");
			assertThat(metadata.getPaymentReference()).isEqualTo("9001011234");
			assertThat(metadata.getTotalAmount()).isEqualTo("1000.00");
		});
		assertThat(item.getStatus()).isEqualTo(UNHANDLED);
	}

	/**
	 * Test scenario where item have incomplete metadata.
	 * <DueDate></DueDate> is missing for the given invoice.
	 */
	@Test
	void extractItemMetadata_2(@Load("/files/ArchiveIndex.xml") final String xml) {
		final var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000002_to_9101011234.pdf"));

		invoiceProcessor.extractItemMetadata(item, xml);

		assertThat(item.getStatus()).isEqualTo(METADATA_INCOMPLETE);
	} 

	@Test
	void validateLegalId() {
		var item1 = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientLegalId("9001011234"));
		var item2 = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientLegalId("2104142399"));

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
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientPartyId("somePartyId"));
		var municipalityId = "someMunicipalityId";
		when(citizenIntegrationMock.hasProtectedIdentity(item.getRecipientPartyId(), municipalityId)).thenReturn(true);

		invoiceProcessor.markProtectedIdentityItems(item, municipalityId);

		assertThat(item.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
	}

	/**
	 * Test scenario where recipient does not have protected identity.
	 */
	@Test
	void markProtectedIdentityItems_2() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientPartyId("somePartyId"));
		var municipalityId = "someMunicipalityId";
		when(citizenIntegrationMock.hasProtectedIdentity(item.getRecipientPartyId(), municipalityId)).thenReturn(false);

		invoiceProcessor.markProtectedIdentityItems(item, municipalityId);

		assertThat(item.getStatus()).isEqualTo(UNHANDLED);
	}

	/**
	 * Test scenario where recipient party id is found.
	 */
	@Test
	void fetchInvoiceRecipientPartyIds_1() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientLegalId("202104142399"));
		when(partyIntegrationMock.getPartyId(item.getRecipientLegalId(), MUNICIPALITY_ID)).thenReturn(Optional.of(new LegalIdAndPartyId("1234", "5678")));

		invoiceProcessor.fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);

		assertThat(item.getRecipientLegalId()).isEqualTo("1234");
		assertThat(item.getRecipientPartyId()).isEqualTo("5678");
		assertThat(item.getStatus()).isEqualTo(RECIPIENT_PARTY_ID_FOUND);
	}

	/**
	 * Test scenario where recipient party id is not found.
	 */
	@Test
	void fetchInvoiceRecipientPartyIds_2() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientLegalId("202104142399"));
		when(partyIntegrationMock.getPartyId(item.getRecipientLegalId(), MUNICIPALITY_ID)).thenReturn(Optional.empty());

		invoiceProcessor.fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);

		assertThat(item.getStatus()).isEqualTo(RECIPIENT_PARTY_ID_NOT_FOUND);
	}

	/**
	 * Test scenario where invoice is sent.
	 */
	@Test
	void sendDigitalInvoices_1() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientPartyId("1234"));

		when(messagingIntegrationMock.sendInvoice(MUNICIPALITY_ID, item)).thenReturn(SENT);

		invoiceProcessor.sendDigitalInvoice(MUNICIPALITY_ID, item);

		assertThat(item.getStatus()).isEqualTo(SENT);
	}

	/**
	 * Test scenario where invoice is not sent.
	 */
	@Test
	void sendDigitalInvoices_2() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setRecipientPartyId("1234"));

		when(messagingIntegrationMock.sendInvoice(MUNICIPALITY_ID, item)).thenReturn(NOT_SENT);

		invoiceProcessor.sendDigitalInvoice(MUNICIPALITY_ID, item);

		assertThat(item.getStatus()).isEqualTo(NOT_SENT);
	}

	@Test
	void extractInvoiceRecipientLegalId_1() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));

		invoiceProcessor.extractInvoiceRecipientLegalId(item);

		assertThat(item.getRecipientLegalId()).isEqualTo("9001011234");
		assertThat(item.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_FOUND);
	}

	@Test
	void extractInvoiceRecipientLegalId_2() {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_.pdf"));

		invoiceProcessor.extractInvoiceRecipientLegalId(item);

		assertThat(item.getStatus()).isEqualTo(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
	}

	@Test
	void updateAndPersistBatch() {
		var batch = createBatchEntity();
		var items = List.of(
			createItemEntity(itemBeingModified -> itemBeingModified.setStatus(SENT)),
			createItemEntity(itemBeingModified -> itemBeingModified.setStatus(IGNORED)));
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
		var invoiceProcessorSpy = spy(invoiceProcessor);

		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(IGNORED)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy, never()).extractItemMetadata(any(), any());
		verify(invoiceProcessorSpy, never()).extractInvoiceRecipientLegalId(any());
		verify(invoiceProcessorSpy, never()).validateLegalId(any());
		verify(invoiceProcessorSpy, never()).markProtectedIdentityItems(any(), eq(MUNICIPALITY_ID));
		verify(invoiceProcessorSpy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(invoiceProcessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the extractItemMetadata check
	 */
	@Test
	void run_2() throws IOException {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProessorSpy = spy(invoiceProcessor);
		runMethodCommonStubs(item, invoiceProessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(updateItem(METADATA_INCOMPLETE)).when(invoiceProessorSpy).extractItemMetadata(item, "mocked-string");

		invoiceProessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProessorSpy, never()).extractInvoiceRecipientLegalId(any());
		verify(invoiceProessorSpy, never()).validateLegalId(any());
		verify(invoiceProessorSpy, never()).markProtectedIdentityItems(any(), eq(MUNICIPALITY_ID));
		verify(invoiceProessorSpy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(invoiceProessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the extractInvoiceRecipientLegalId check
	 */
	@Test
	void run_3() throws IOException {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProcessorSpy = spy(invoiceProcessor);

		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		verify(invoiceProcessorSpy, never()).validateLegalId(any());
		verify(invoiceProcessorSpy, never()).markProtectedIdentityItems(any(), eq(MUNICIPALITY_ID));
		verify(invoiceProcessorSpy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(invoiceProcessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the validateLegalId check
	 */
	@Test
	void run_4() throws IOException {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProcessorSpy = spy(invoiceProcessor);

		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(invoiceProcessorSpy).validateLegalId(item);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		verify(invoiceProcessorSpy).validateLegalId(item);
		verify(invoiceProcessorSpy, never()).markProtectedIdentityItems(any(), eq(MUNICIPALITY_ID));
		verify(invoiceProcessorSpy, never()).fetchInvoiceRecipientPartyIds(any(), any());
		verify(invoiceProcessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the markProtectedIdentityItems check
	 */
	@Test
	void run_5() throws IOException {

		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProcessorSpy = spy(invoiceProcessor);
		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).validateLegalId(item);
		doAnswer(updateItem(RECIPIENT_PARTY_ID_FOUND)).when(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)).when(invoiceProcessorSpy).markProtectedIdentityItems(item, MUNICIPALITY_ID);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		verify(invoiceProcessorSpy).validateLegalId(item);
		verify(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(any(), any());
		verify(invoiceProcessorSpy).markProtectedIdentityItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the fetchInvoiceRecipientPartyIds check
	 */
	@Test
	void run_6() throws IOException {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProcessorSpy = spy(invoiceProcessor);
		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).validateLegalId(item);
		doAnswer(updateItem(RECIPIENT_PARTY_ID_NOT_FOUND)).when(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		verify(invoiceProcessorSpy).validateLegalId(item);
		verify(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy, never()).markProtectedIdentityItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy, never()).sendDigitalInvoice(any(), any());
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Test the scenario where the invoice fails the sendDigitalInvoices check
	 */
	@Test
	void run_7() throws IOException {
		var item = createItemEntity(itemBeingModified -> itemBeingModified.setFilename("Faktura_00000001_to_9001011234.pdf"));
		var invoiceProcessorSpy = spy(invoiceProcessor);
		runMethodCommonStubs(item, invoiceProcessorSpy);

		doAnswer(updateItem(INVOICE, IN_PROGRESS)).when(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		doAnswer(updateItem(RECIPIENT_LEGAL_ID_FOUND)).when(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).validateLegalId(item);
		doAnswer(doNotUpdate()).when(invoiceProcessorSpy).markProtectedIdentityItems(item, MUNICIPALITY_ID);
		doAnswer(updateItem(RECIPIENT_PARTY_ID_FOUND)).when(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		doAnswer(updateItem(NOT_SENT)).when(invoiceProcessorSpy).sendDigitalInvoice(MUNICIPALITY_ID, item);

		invoiceProcessorSpy.run(LocalDate.now(), MUNICIPALITY_ID, "BatchName");

		verify(invoiceProcessorSpy).markItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).extractItemMetadata(item, "mocked-string");
		verify(invoiceProcessorSpy).extractInvoiceRecipientLegalId(item);
		verify(invoiceProcessorSpy).validateLegalId(item);
		verify(invoiceProcessorSpy).markProtectedIdentityItems(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).fetchInvoiceRecipientPartyIds(item, MUNICIPALITY_ID);
		verify(invoiceProcessorSpy).sendDigitalInvoice(MUNICIPALITY_ID, item);
		verify(dbIntegrationMock).persistItem(item);
	}

	/**
	 * Some methods update the ItemType and ItemStatus of the item
	 */
	private Answer<ItemEntity> updateItem(final ItemType type, final ItemStatus status) {
		return invocation -> {
			var invocationArgument = (ItemEntity) invocation.getArgument(0);
			ofNullable(type).ifPresent(invocationArgument::setType);
			ofNullable(status).ifPresent(invocationArgument::setStatus);
			return null;
		};
	}

	/**
	 * Some methods only updates the status of the item.
	 */
	private Answer<ItemEntity> updateItem(final ItemStatus status) {
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
		var batch = createBatchEntity(batchBeingModified -> batchBeingModified.withItems(List.of(item)));
		var batches = List.of(batch);

		var raindanceIntegration = mock(RaindanceIntegration.class);
		var raindanceIntegrations = Map.of(MUNICIPALITY_ID, raindanceIntegration);
		ReflectionTestUtils.setField(invoiceProcessor, "raindanceIntegrations", raindanceIntegrations);

		when(raindanceIntegration.readBatches(date, "BatchName", "2281")).thenReturn(batches);
		when(dbIntegrationMock.persistBatches(batches)).thenReturn(batches);
		doNothing().when(raindanceIntegration).writeBatch(batch);
		doNothing().when(raindanceIntegration).archiveOriginalBatch(batch);
		doNothing().when(invoiceProcessor).updateAndPersistBatch(batch);
		lenient().doReturn("mocked-string").when(invoiceProcessor).removeItemFromArchiveIndex(item, "mocked-string");
		doNothing().when(messagingIntegrationMock).sendStatusReport(batches, date, MUNICIPALITY_ID);
	}
}
