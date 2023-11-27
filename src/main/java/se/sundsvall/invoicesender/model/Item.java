package se.sundsvall.invoicesender.model;

public class Item {

    private String zipFilename;
    private String recipientLegalId;
    private String recipientPartyId;
    private String filename;
    private Status status = Status.UNHANDLED;
    private Metadata metadata;

    public String getZipFilename() {
        return zipFilename;
    }

    public Item setZipFilename(final String zipFilename) {
        this.zipFilename = zipFilename;
        return this;
    }

    public String getRecipientLegalId() {
        return recipientLegalId;
    }

    public Item setRecipientLegalId(final String recipientLegalId) {
        this.recipientLegalId = recipientLegalId;
        return this;
    }

    public String getRecipientPartyId() {
        return recipientPartyId;
    }

    public Item setRecipientPartyId(final String recipientPartyId) {
        this.recipientPartyId = recipientPartyId;
        return this;
    }

    public String getFilename() {
        return filename;
    }

    public Item setFilename(final String filename) {
        this.filename = filename;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Item setStatus(final Status status) {
        this.status = status;
        return this;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public Item setMetadata(final Metadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public static class Metadata {

        private String invoiceNumber;
        private String invoiceDate;
        private String dueDate;
        private String paymentNumber;
        private String paymentReference;
        private String totalAmount;
        private boolean reminder;

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public Metadata setInvoiceNumber(final String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
            return this;
        }

        public String getInvoiceDate() {
            return invoiceDate;
        }

        public Metadata setInvoiceDate(final String invoiceDate) {
            this.invoiceDate = invoiceDate;
            return this;
        }

        public String getDueDate() {
            return dueDate;
        }

        public Metadata setDueDate(final String dueDate) {
            this.dueDate = dueDate;
            return this;
        }

        public String getPaymentNumber() {
            return paymentNumber;
        }

        public Metadata setPaymentNumber(final String paymentNumber) {
            this.paymentNumber = paymentNumber;
            return this;
        }

        public String getPaymentReference() {
            return paymentReference;
        }

        public Metadata setPaymentReference(final String paymentReference) {
            this.paymentReference = paymentReference;
            return this;
        }

        public String getTotalAmount() {
            return totalAmount;
        }

        public Metadata setTotalAmount(final String totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public boolean isReminder() {
            return reminder;
        }

        public Metadata setReminder(final boolean reminder) {
            this.reminder = reminder;
            return this;
        }
    }
}
