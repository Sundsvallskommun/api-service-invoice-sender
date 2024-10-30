package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import se.sundsvall.invoicesender.model.ItemStatus;

@Entity
@Table(name = "batch_items")
public class ItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "filename", nullable = false)
	private String filename;

	@Enumerated(STRING)
	@Column(name = "status", nullable = false)
	private ItemStatus status;

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
}
