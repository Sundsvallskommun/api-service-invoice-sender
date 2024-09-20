package se.sundsvall.invoicesender.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.invoicesender.model.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.model.ItemStatus.NOT_SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_NOT_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;
import static se.sundsvall.invoicesender.model.ItemStatus.UNHANDLED;
import static se.sundsvall.invoicesender.model.ItemType.INVOICE;
import static se.sundsvall.invoicesender.model.ItemType.OTHER;
import static se.sundsvall.invoicesender.model.ItemType.UNKNOWN;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.invoicesender.integration.db.DbIntegration;
import se.sundsvall.invoicesender.integration.db.dto.BatchDto;
import se.sundsvall.invoicesender.integration.messaging.MessagingIntegration;
import se.sundsvall.invoicesender.integration.party.PartyIntegration;
import se.sundsvall.invoicesender.integration.raindance.RaindanceIntegration;
import se.sundsvall.invoicesender.model.Batch;
import se.sundsvall.invoicesender.model.Item;

@ExtendWith(MockitoExtension.class)
class InvoiceProcessorTests {

	@Mock
	private RaindanceIntegration mockRaindanceIntegration;

	@Mock
	private PartyIntegration mockPartyIntegration;

	@Mock
	private MessagingIntegration mockMessagingIntegration;

	@Mock
	private DbIntegration mockDbIntegration;

	private InvoiceProcessor invoiceProcessor;

	@BeforeEach
	void setUp() {
		invoiceProcessor = new InvoiceProcessor(mockRaindanceIntegration, mockPartyIntegration,
			mockMessagingIntegration, mockDbIntegration, List.of("Faktura"), "-");
	}

	@Test
	void markNonPdfItemsAsOther() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.txt"),
				new Item("file2.pdf"),
				new Item("file3.zip"),
				new Item("file4.docx"),
				new Item("file5.pdf")
			));

		invoiceProcessor.markNonPdfItemsAsOther(batch);

		assertThat(batch.getItems())
			.extracting(Item::getType)
			.containsExactlyInAnyOrder(OTHER, UNKNOWN, OTHER, OTHER, UNKNOWN);
	}

	@Test
	void markInvoiceItems() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.txt"),
				new Item("file2.pdf"),
				new Item("file3.zip"),
				new Item("file4.docx"),
				new Item("file5.pdf")
			));

		invoiceProcessor.markNonPdfItemsAsOther(batch);
		invoiceProcessor.markInvoiceItems(batch);

		assertThat(batch.getItems())
			.extracting(Item::getType)
			.containsExactlyInAnyOrder(OTHER, INVOICE, OTHER, OTHER, INVOICE);
	}

	@Test
	void extractInvoiceRecipientLegalIds() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1").withType(OTHER),
				new Item("file2").withType(INVOICE).withFilename("somePrefix_123_to_456.pdf"),
				new Item("file3").withType(INVOICE).withFilename("somePrefix_456_to_789.pdf"),
				new Item("file4").withType(INVOICE).withFilename("somePrefix_abc_to_456.pdf")
			));

		invoiceProcessor.extractInvoiceRecipientLegalIds(batch);

		assertThat(batch.getItems())
			.extracting(Item::getStatus)
			.containsExactlyInAnyOrder(UNHANDLED, RECIPIENT_LEGAL_ID_FOUND, RECIPIENT_LEGAL_ID_FOUND, RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		assertThat(batch.getItems())
			.extracting(Item::getRecipientLegalId)
			.containsExactlyInAnyOrder(null, "456", "789", null);

		verifyNoInteractions(mockRaindanceIntegration, mockPartyIntegration, mockDbIntegration, mockMessagingIntegration);
	}

	@Test
	void fetchInvoiceRecipientPartyIds() {
		final var batch = new Batch()
			.withLocalPath("somePath")
			.withItems(List.of(
				new Item("file1.pdf")
					.withType(INVOICE)
					.withStatus(RECIPIENT_LEGAL_ID_FOUND)
					.withRecipientLegalId("legalId1"),
				new Item("file2.pdf")
					.withType(INVOICE)
					.withStatus(RECIPIENT_LEGAL_ID_FOUND)
					.withRecipientLegalId("legalId1"),
				new Item("file3.pdf").withType(INVOICE),
				new Item("file4.pdf")
					.withType(INVOICE)
					.withStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID)
					.withRecipientLegalId("legalId3")
			));

		when(mockPartyIntegration.getPartyId(any(String.class), any(String.class)))
			.thenReturn(Optional.of("somePartyId"))
			.thenReturn(Optional.empty());

		invoiceProcessor.fetchInvoiceRecipientPartyIds(batch, "2281");

		assertThat(batch.getItems())
			.extracting(Item::getStatus)
			.containsExactlyInAnyOrder(RECIPIENT_PARTY_ID_FOUND, RECIPIENT_PARTY_ID_NOT_FOUND, UNHANDLED, RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID);
		assertThat(batch.getItems())
			.extracting(Item::getRecipientPartyId)
			.containsExactlyInAnyOrder("somePartyId", null, null, null);

		verify(mockPartyIntegration, times(2)).getPartyId(any(String.class), any(String.class));
		verifyNoMoreInteractions(mockPartyIntegration);
		verifyNoInteractions(mockDbIntegration, mockMessagingIntegration, mockRaindanceIntegration);
	}

	@Test
	void sendDigitalInvoices() {
		final var batch = new Batch()
			.withLocalPath("somePath")
			.withItems(List.of(
				new Item("file1.pdf")
					.withType(INVOICE)
					.withStatus(RECIPIENT_PARTY_ID_FOUND)
					.withRecipientPartyId("partyId1"),
				new Item("file2.pdf").withType(INVOICE),
				new Item("file3.pdf")
					.withType(INVOICE)
					.withStatus(RECIPIENT_PARTY_ID_FOUND)
					.withRecipientPartyId("partyId3")
			));
		final var municipalityId = "someMunicipalityId";

		when(mockMessagingIntegration.sendInvoice(any(String.class), any(Item.class), any(String.class)))
			.thenReturn(SENT, NOT_SENT);

		invoiceProcessor.sendDigitalInvoices(batch, municipalityId);

		assertThat(batch.getItems())
			.extracting(Item::getStatus)
			.containsExactlyInAnyOrder(SENT, UNHANDLED, NOT_SENT);

		verify(mockMessagingIntegration, times(2)).sendInvoice(any(String.class), any(Item.class), any(String.class));
		verifyNoMoreInteractions(mockMessagingIntegration);
		verifyNoInteractions(mockPartyIntegration, mockDbIntegration, mockRaindanceIntegration);
	}

	@Test
	void completeBatchAndStoreExecution() {
		final var batchCaptor = ArgumentCaptor.forClass(Batch.class);

		when(mockDbIntegration.storeBatch(any(Batch.class), any(String.class)))
			.thenReturn(new BatchDto(123, "basename", LocalDateTime.now(), LocalDateTime.now(), 5, 3, false));

		final var result = invoiceProcessor.completeBatchAndStoreExecution(new Batch(), "2281");

		assertThat(result).isNotNull();
		verify(mockDbIntegration).storeBatch(batchCaptor.capture(), any(String.class));
		verifyNoMoreInteractions(mockDbIntegration);
		verifyNoInteractions(mockRaindanceIntegration, mockPartyIntegration, mockMessagingIntegration);

		assertThat(batchCaptor.getValue().getCompletedAt()).isNotNull();
	}

	@Test
	void getItems() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1").withRecipientPartyId("partyId1"),
				new Item("file2").withRecipientPartyId("partyId2")
			));

		final var result = invoiceProcessor.getItems(batch, item -> "partyId1".equals(item.getRecipientPartyId()));

		assertThat(result).isNotNull().hasSize(1);
	}

	@Test
	void getProcessableInvoiceItems() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.png").withType(OTHER),
				new Item("file2.pdf").withType(INVOICE),
				new Item("file3.pdf").withType(INVOICE),
				new Item("file4.pdf").withType(INVOICE).withStatus(IGNORED)
			));

		final var result = invoiceProcessor.getProcessableInvoiceItems(batch);

		assertThat(result).isNotNull().hasSize(2);
	}

	@Test
	void getSentInvoiceItems() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.jpg").withType(OTHER),
				new Item("file2.pdf").withType(INVOICE).withStatus(SENT),
				new Item("file3.pdf").withType(INVOICE).withStatus(SENT),
				new Item("file4.pdf").withType(INVOICE).withStatus(IGNORED),
				new Item("file5.pdf").withType(INVOICE).withStatus(NOT_SENT)
			));

		final var result = invoiceProcessor.getSentInvoiceItems(batch);

		assertThat(result).isNotNull().hasSize(2);
	}

	@Test
	void getInvoiceItemsWithPartyIdSet() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.txt").withType(OTHER),
				new Item("file2.pdf").withType(INVOICE).withStatus(RECIPIENT_PARTY_ID_FOUND),
				new Item("file3.pdf").withType(INVOICE).withStatus(RECIPIENT_PARTY_ID_FOUND),
				new Item("file4.pdf").withType(INVOICE).withStatus(RECIPIENT_PARTY_ID_NOT_FOUND),
				new Item("file5.pdf").withType(INVOICE)
			));

		final var result = invoiceProcessor.getInvoiceItemsWithPartyIdSet(batch);

		assertThat(result).isNotNull().hasSize(2);
	}

	@Test
	void getInvoiceItemsWithLegalIdSet() {
		final var batch = new Batch()
			.withItems(List.of(
				new Item("file1.docx").withType(OTHER),
				new Item("file2.pdf").withType(INVOICE).withStatus(RECIPIENT_LEGAL_ID_FOUND),
				new Item("file3.pdf").withType(INVOICE).withStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID),
				new Item("file4.pdf").withType(INVOICE).withStatus(RECIPIENT_LEGAL_ID_NOT_FOUND_OR_INVALID),
				new Item("file5.pdf").withType(INVOICE)
			));

		final var result = invoiceProcessor.getInvoiceItemsWithLegalIdSet(batch);

		assertThat(result).isNotNull().hasSize(1);
	}

}
