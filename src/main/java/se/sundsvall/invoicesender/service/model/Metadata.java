package se.sundsvall.invoicesender.service.model;

import java.util.Objects;

public class Metadata {

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
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Metadata metadata = (Metadata) o;
		return payable == metadata.payable && reminder == metadata.reminder && Objects.equals(invoiceNumber, metadata.invoiceNumber) && Objects.equals(invoiceDate, metadata.invoiceDate) && Objects.equals(dueDate, metadata.dueDate)
			&& Objects.equals(accountNumber, metadata.accountNumber) && Objects.equals(paymentReference, metadata.paymentReference) && Objects.equals(totalAmount, metadata.totalAmount);
	}

	@Override
	public int hashCode() {
		return Objects.hash(invoiceNumber, invoiceDate, dueDate, accountNumber, paymentReference, totalAmount, payable, reminder);
	}

	@Override
	public String toString() {
		return "Metadata{" +
			"invoiceNumber='" + invoiceNumber + '\'' +
			", invoiceDate='" + invoiceDate + '\'' +
			", dueDate='" + dueDate + '\'' +
			", accountNumber='" + accountNumber + '\'' +
			", paymentReference='" + paymentReference + '\'' +
			", totalAmount='" + totalAmount + '\'' +
			", payable=" + payable +
			", reminder=" + reminder +
			'}';
	}
}
