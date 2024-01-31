package se.sundsvall.invoicesender.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Batch {

    private String path;
    private String basename;
    private byte[] data;
    private String remotePath;
    private List<Item> items;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public Batch withPath(final String path) {
        this.path = path;
        return this;
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

    public String getRemotePath() {
        return remotePath;
    }

    public Batch withRemotePath(final String remotePath) {
        this.remotePath = remotePath;
        return this;
    }

    public void setRemotePath(final String remotePath) {
        this.remotePath = remotePath;
    }

    public List<Item> getItems() {
        return items;
    }

    public Batch addItem(final Item item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        return this;
    }

    public Batch withItems(final List<Item> items) {
        this.items = items;
        return this;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
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
