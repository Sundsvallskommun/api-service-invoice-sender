package se.sundsvall.invoicesender.model;

import static java.util.function.Predicate.not;
import static se.sundsvall.invoicesender.model.ItemStatus.IGNORED;
import static se.sundsvall.invoicesender.model.ItemStatus.METADATA_INCOMPLETE;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_LEGAL_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.RECIPIENT_PARTY_ID_FOUND;
import static se.sundsvall.invoicesender.model.ItemStatus.SENT;
import static se.sundsvall.invoicesender.model.ItemType.INVOICE;

import java.util.Objects;
import java.util.function.Predicate;

public class Item {

    public static final Predicate<Item> ITEM_IS_A_PDF = item -> item.getFilename().toLowerCase().endsWith(".pdf");
    public static final Predicate<Item> ITEM_IS_AN_INVOICE = ITEM_IS_A_PDF.and(item -> item.getType() == INVOICE);
    public static final Predicate<Item> ITEM_IS_IGNORED = item -> item.getStatus() == IGNORED;
    public static final Predicate<Item> ITEM_LACKS_METADATA = item -> item.getStatus() == METADATA_INCOMPLETE;
    public static final Predicate<Item> ITEM_IS_PROCESSABLE = ITEM_IS_AN_INVOICE.and(not(ITEM_IS_IGNORED)).and(not(ITEM_LACKS_METADATA));
    public static final Predicate<Item> ITEM_HAS_LEGAL_ID = item -> item.getStatus() == RECIPIENT_LEGAL_ID_FOUND;
    public static final Predicate<Item> ITEM_HAS_PARTY_ID = item -> item.getStatus() == RECIPIENT_PARTY_ID_FOUND;
    public static final Predicate<Item> ITEM_IS_SENT = item -> item.getStatus() == SENT;

    private ItemType type = ItemType.UNKNOWN;
    private ItemStatus status = ItemStatus.UNHANDLED;
    private String filename;
    private Metadata metadata;
    private String recipientLegalId;
    private String recipientPartyId;

    public Item() {
    }

    public Item(final String filename) {
        this.filename = filename;
    }

    public ItemType getType() {
        return type;
    }

    public Item withType(final ItemType type) {
        this.type = type;
        return this;
    }

    public void setType(final ItemType type) {
        this.type = type;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Item withStatus(final ItemStatus status) {
        this.status = status;
        return this;
    }

    public void setStatus(final ItemStatus status) {
        this.status = status;
    }

    public String getFilename() {
        return filename;
    }

    public Item withFilename(final String filename) {
        this.filename = filename;
        return this;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Item withMetadata(final Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public void setMetadata(final Metadata metadata) {
        this.metadata = metadata;
    }

    public String getRecipientLegalId() {
        return recipientLegalId;
    }

    public Item withRecipientLegalId(final String recipientLegalId) {
        this.recipientLegalId = recipientLegalId;
        return this;
    }

    public void setRecipientLegalId(final String recipientLegalId) {
        this.recipientLegalId = recipientLegalId;
    }

    public String getRecipientPartyId() {
        return recipientPartyId;
    }

    public Item withRecipientPartyId(final String recipientPartyId) {
        this.recipientPartyId = recipientPartyId;
        return this;
    }

    public void setRecipientPartyId(final String recipientPartyId) {
        this.recipientPartyId = recipientPartyId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return type == item.type &&
            status == item.status &&
            Objects.equals(filename, item.filename) &&
            Objects.equals(metadata, item.metadata) &&
            Objects.equals(recipientLegalId, item.recipientLegalId) &&
            Objects.equals(recipientPartyId, item.recipientPartyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, status, filename, metadata, recipientLegalId, recipientPartyId);
    }

    public static class Metadata {

        private String invoiceNumber;
        private String invoiceDate;
        private String dueDate;
        private String accountNumber;
        private String paymentReference;
        private String totalAmount;
        private boolean payable;
        private boolean reminder;

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public Metadata withInvoiceNumber(final String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public void setInvoiceNumber(final String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
        }

        public String getInvoiceDate() {
            return invoiceDate;
        }

        public Metadata withInvoiceDate(final String invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public void setInvoiceDate(final String invoiceDate) {
            this.invoiceDate = invoiceDate;
        }

        public String getDueDate() {
            return dueDate;
        }

        public Metadata withDueDate(final String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public void setDueDate(final String dueDate) {
            this.dueDate = dueDate;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public Metadata withAccountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
            return this;
        }

        public void setAccountNumber(final String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public Metadata withPaymentReference(final String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }

        public void setPaymentReference(final String paymentReference) {
            this.paymentReference = paymentReference;
        }

        public String getTotalAmount() {
            return totalAmount;
        }

        public Metadata withTotalAmount(final String totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public void setTotalAmount(final String totalAmount) {
            this.totalAmount = totalAmount;
        }

        public boolean isPayable() {
            return payable;
        }

        public Metadata withPayable(final boolean payable) {
            this.payable = payable;
            return this;
        }

        public void setPayable(final boolean payable) {
            this.payable = payable;
        }

        public boolean isReminder() {
            return reminder;
        }

        public Metadata withReminder(final boolean reminder) {
            this.reminder = reminder;
            return this;
        }

        public void setReminder(final boolean reminder) {
            this.reminder = reminder;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Metadata metadata)) {
                return false;
            }
            return payable == metadata.payable &&
                reminder == metadata.reminder &&
                Objects.equals(invoiceNumber, metadata.invoiceNumber) &&
                Objects.equals(invoiceDate, metadata.invoiceDate) &&
                Objects.equals(dueDate, metadata.dueDate) &&
                Objects.equals(accountNumber, metadata.accountNumber) &&
                Objects.equals(paymentReference, metadata.paymentReference) &&
                Objects.equals(totalAmount, metadata.totalAmount);
        }

        @Override
        public int hashCode() {
            return Objects.hash(invoiceNumber, invoiceDate, dueDate, accountNumber, paymentReference, totalAmount, payable, reminder);
        }
    }
}
