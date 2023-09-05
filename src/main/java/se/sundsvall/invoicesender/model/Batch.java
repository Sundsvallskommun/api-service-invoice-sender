package se.sundsvall.invoicesender.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Batch {

    private String path;
    private String basename;
    private String remotePath;
    private List<Item> items;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public String getPath() {
        return path;
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

    public String getRemotePath() {
        return remotePath;
    }

    public Batch withRemotePath(final String remotePath) {
        this.remotePath = remotePath;
        return this;
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
