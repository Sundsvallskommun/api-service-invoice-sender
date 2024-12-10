package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.EAGER;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "batch_executions",
	indexes = {
		@Index(name = "idx_batch_executions_municipality_id", columnList = "municipality_id")
	})
public class BatchEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "basename", nullable = false)
	private String basename;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt = LocalDateTime.now();

	@Column(name = "completed_at", nullable = false)
	private LocalDateTime completedAt;

	@OneToMany(fetch = EAGER, cascade = PERSIST)
	@JoinColumn(name = "batch_id", nullable = false)
	private List<ItemEntity> items;

	@Column(name = "total_invoices", nullable = false)
	private long totalItems;

	@Column(name = "ignored_invoices", nullable = false)
	private long ignoredItems;

	@Column(name = "sent_invoices", nullable = false)
	private long sentItems;

	public Integer getId() {
		return id;
	}

	public void setId(final Integer id) {
		this.id = id;
	}

	public BatchEntity withId(final Integer id) {
		this.id = id;
		return this;
	}

	public String getBasename() {
		return basename;
	}

	public void setBasename(final String basename) {
		this.basename = basename;
	}

	public BatchEntity withBasename(final String basename) {
		this.basename = basename;
		return this;
	}

	public String getMunicipalityId() {
		return municipalityId;
	}

	public void setMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
	}

	public BatchEntity withMunicipalityId(final String municipalityId) {
		this.municipalityId = municipalityId;
		return this;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(final LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public BatchEntity withStartedAt(final LocalDateTime startedAt) {
		this.startedAt = startedAt;
		return this;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(final LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public BatchEntity withCompletedAt(final LocalDateTime completedAt) {
		this.completedAt = completedAt;
		return this;
	}

	public List<ItemEntity> getItems() {
		return items;
	}

	public void setItems(final List<ItemEntity> items) {
		this.items = items;
	}

	public BatchEntity withItems(final List<ItemEntity> items) {
		this.items = items;
		return this;
	}

	public long getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(final long totalItems) {
		this.totalItems = totalItems;
	}

	public BatchEntity withTotalItems(final long totalItems) {
		this.totalItems = totalItems;
		return this;
	}

	public long getIgnoredItems() {
		return ignoredItems;
	}

	public void setIgnoredItems(final long ignoredItems) {
		this.ignoredItems = ignoredItems;
	}

	public BatchEntity withIgnoredItems(final long ignoredItems) {
		this.ignoredItems = ignoredItems;
		return this;
	}

	public long getSentItems() {
		return sentItems;
	}

	public void setSentItems(final long sentItems) {
		this.sentItems = sentItems;
	}

	public BatchEntity withSentItems(final long sentItems) {
		this.sentItems = sentItems;
		return this;
	}

}
