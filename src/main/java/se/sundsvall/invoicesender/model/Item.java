package se.sundsvall.invoicesender.model;

public class Item {

    private String zipFilename;
    private String recipientLegalId;
    private String recipientPartyId;
    private String filename;
    private Status status = Status.UNHANDLED;

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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Invoice{");
        sb.append("recipientLegalId='").append(recipientLegalId).append('\'');
        sb.append(", zipFilename='").append(zipFilename).append('\'');
        sb.append(", filename='").append(filename).append('\'');
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
