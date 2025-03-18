package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import se.sundsvall.invoicesender.service.model.Metadata;

@Entity
@Table(name = "batch_items")
public class ItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "filename", nullable = false)
	private String filename;

	@Lob
	@Basic(fetch = LAZY)
	@Column(name = "data", columnDefinition = "LONGBLOB")
	private byte[] data;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false)
	private ItemStatus status;

	@Enumerated(STRING)
	@Column(name = "type", nullable = false)
	private ItemType type;

	@Transient
	private Metadata metadata;

	@Transient
	private String recipientPartyId;

	@Transient
	private String recipientLegalId;

	public Integer getId() {
		return id;
	}

	public ItemEntity withId(final Integer id) {
		this.id = id;
		return this;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public String getFilename() {
		return filename;
	}

	public ItemEntity withFilename(final String filename) {
		this.filename = filename;
		return this;
	}

	public void setFilename(final String filename) {
		this.filename = filename;
	}

	public byte[] getData() {
		return data;
	}

	public ItemEntity withData(final byte[] data) {
		this.data = data;
		return this;
	}

	public void setData(final byte[] data) {
		this.data = data;
	}

	public ItemStatus getStatus() {
		return status;
	}

	public ItemEntity withStatus(final ItemStatus status) {
		this.status = status;
		return this;
	}

	public void setStatus(final ItemStatus status) {
		this.status = status;
	}

	public ItemType getType() {
		return type;
	}

	public ItemEntity withType(final ItemType type) {
		this.type = type;
		return this;
	}

	public void setType(final ItemType type) {
		this.type = type;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public ItemEntity withMetadata(final Metadata metadata) {
		this.metadata = metadata;
		return this;
	}

	public void setMetadata(final Metadata metadata) {
		this.metadata = metadata;
	}

	public String getRecipientPartyId() {
		return recipientPartyId;
	}

	public ItemEntity withRecipientPartyId(final String recipientPartyId) {
		this.recipientPartyId = recipientPartyId;
		return this;
	}

	public void setRecipientPartyId(final String recipientPartyId) {
		this.recipientPartyId = recipientPartyId;
	}

	public String getRecipientLegalId() {
		return recipientLegalId;
	}

	public void setRecipientLegalId(final String recipientLegalId) {
		this.recipientLegalId = recipientLegalId;
	}

	public ItemEntity withRecipientLegalId(final String recipientLegalId) {
		this.recipientLegalId = recipientLegalId;
		return this;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof ItemEntity other) {
			return id != null && id.equals(other.id);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public String toString() {
		return "ItemEntity{" +
			"id=" + id +
			", filename='" + filename + '\'' +
			", status=" + status +
			", type=" + type +
			", metadata=" + metadata +
			", recipientPartyId='" + recipientPartyId + '\'' +
			", recipientLegalId='" + recipientLegalId + '\'' +
			'}';
	}
}
