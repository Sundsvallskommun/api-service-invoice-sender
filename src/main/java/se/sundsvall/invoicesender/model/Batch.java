package se.sundsvall.invoicesender.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Batch {

	private String localPath;
	private String targetPath;
	private boolean processingEnabled;
	private String archivePath;
	private String basename;
	private byte[] data;
	private final List<Item> items = new ArrayList<>();
	private final LocalDateTime startedAt = LocalDateTime.now();
	private LocalDateTime completedAt;

	public String getLocalPath() {
		return localPath;
	}

	public Batch withLocalPath(final String localPath) {
		this.localPath = localPath;
		return this;
	}

	public void setLocalPath(final String localPath) {
		this.localPath = localPath;
	}

	public String getTargetPath() {
		return targetPath;
	}

	public Batch withTargetPath(final String targetPath) {
		this.targetPath = targetPath;
		return this;
	}

	public void setTargetPath(final String targetPath) {
		this.targetPath = targetPath;
	}

	public boolean isProcessingEnabled() {
		return processingEnabled;
	}

	public Batch withProcess(final boolean processingEnabled) {
		this.processingEnabled = processingEnabled;
		return this;
	}

	public void setProcessingEnabled(final boolean processingEnabled) {
		this.processingEnabled = processingEnabled;
	}

	public String getArchivePath() {
		return archivePath;
	}

	public Batch withArchivePath(final String archivePath) {
		this.archivePath = archivePath;
		return this;
	}

	public void setArchivePath(final String archivePath) {
		this.archivePath = archivePath;
	}

	public String getBasename() {
		return basename;
	}

	public Batch withBasename(final String basename) {
		this.basename = basename;
		return this;
	}

	public void setBasename(final String basename) {
		this.basename = basename;
	}

	public byte[] getData() {
		return data;
	}

	public Batch withData(final byte[] data) {
		this.data = data;
		return this;
	}

	public void setData(final byte[] data) {
		this.data = data;
	}

	public List<Item> getItems() {
		return items;
	}

	public Batch addItem(final Item item) {
		items.add(item);
		return this;
	}

	public Batch removeItem(final Item item) {
		items.remove(item);
		return this;
	}

	public Batch withItems(final List<Item> items) {
		setItems(items);
		return this;
	}

	public void setItems(final List<Item> items) {
		this.items.clear();
		this.items.addAll(items);
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompleted() {
		completedAt = LocalDateTime.now();
	}
}
