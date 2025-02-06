package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.EnumType.STRING;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import se.sundsvall.invoicesender.service.model.Metadata;

@Entity
@Table(name = "batch_items")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "filename", nullable = false)
	private String filename;

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

	public String getRecipientPartyId() {
		return recipientPartyId;
	}

	public void setRecipientPartyId(String recipientPartyId) {
		this.recipientPartyId = recipientPartyId;
	}

	public ItemEntity withRecipientPartyId(String recipientPartyId) {
		this.recipientPartyId = recipientPartyId;
		return this;
	}

	public String getRecipientLegalId() {
		return recipientLegalId;
	}

	public void setRecipientLegalId(String recipientLegalId) {
		this.recipientLegalId = recipientLegalId;
	}

	public ItemEntity withRecipientLegalId(String recipientLegalId) {
		this.recipientLegalId = recipientLegalId;
		return this;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public ItemEntity withMetadata(Metadata metadata) {
		this.metadata = metadata;
		return this;
	}

	public ItemType getType() {
		return type;
	}

	public void setType(ItemType type) {
		this.type = type;
	}

	public ItemEntity withType(ItemType type) {
		this.type = type;
		return this;
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
