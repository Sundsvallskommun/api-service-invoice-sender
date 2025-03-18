package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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

	@Column(name = "filename", nullable = false)
	private String filename;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "archive_path")
	private String archivePath;

	@Column(name = "target_path")
	private String targetPath;

	@Column(name = "date")
	private LocalDate date;

	@Column(name = "started_at", nullable = false)
	private LocalDateTime startedAt = LocalDateTime.now();

	@Column(name = "completed_at")
	private LocalDateTime completedAt;

	@OneToMany(fetch = LAZY, cascade = PERSIST)
	@JoinColumn(name = "batch_id", nullable = false)
	private List<ItemEntity> items = new ArrayList<>();

	@Column(name = "total_invoices", nullable = false)
	private long totalItems;

	@Column(name = "ignored_invoices", nullable = false)
	private long ignoredItems;

	@Column(name = "sent_invoices", nullable = false)
	private long sentItems;

	@Column(name = "processing_enabled", nullable = false)
	private boolean processingEnabled;

	@Column(name = "completed", nullable = false)
	private boolean completed;

	@Lob
	@Basic(fetch = LAZY)
	@Column(name = "data", columnDefinition = "LONGBLOB")
	private byte[] data;

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

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public BatchEntity withDate(LocalDate date) {
		this.date = date;
		return this;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public BatchEntity withData(byte[] data) {
		this.data = data;
		return this;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public BatchEntity withCompleted(boolean completed) {
		this.completed = completed;
		return this;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public BatchEntity withTargetPath(String targetPath) {
		this.targetPath = targetPath;
		return this;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(final String filename) {
		this.filename = filename;
	}

	public BatchEntity withFilename(final String filename) {
		this.filename = filename;
		return this;
	}

	public String getArchivePath() {
		return archivePath;
	}

	public void setArchivePath(final String archivePath) {
		this.archivePath = archivePath;
	}

	public BatchEntity withArchivePath(final String archivePath) {
		this.archivePath = archivePath;
		return this;
	}

	public boolean isProcessingEnabled() {
		return processingEnabled;
	}

	public void setProcessingEnabled(final boolean processingEnabled) {
		this.processingEnabled = processingEnabled;
	}

	public BatchEntity withProcessingEnabled(final boolean processingEnabled) {
		this.processingEnabled = processingEnabled;
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

	@Override
	public String toString() {
		return "BatchEntity{" +
			"id=" + id +
			", filename='" + filename + '\'' +
			", municipalityId='" + municipalityId + '\'' +
			", archivePath='" + archivePath + '\'' +
			", targetPath='" + targetPath + '\'' +
			", startedAt=" + startedAt +
			", completedAt=" + completedAt +
			", items=" + items +
			", totalItems=" + totalItems +
			", ignoredItems=" + ignoredItems +
			", sentItems=" + sentItems +
			", processingEnabled=" + processingEnabled +
			", completed=" + completed +
			", data=" + Arrays.toString(data) +
			'}';
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof BatchEntity other) {
			return id != null && id.equals(other.id);
		}

		return false;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}
}
