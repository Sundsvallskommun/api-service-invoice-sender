package se.sundsvall.invoicesender.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Batch {

    private String path;
    private String sevenZipFilename;
    private List<Item> items;
    private final LocalDateTime startedAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    public String getPath() {
        return path;
    }

    public Batch setPath(final String path) {
        this.path = path;
        return this;
    }

    public String getSevenZipFilename() {
        return sevenZipFilename;
    }

    public Batch setSevenZipFilename(final String sevenZipFilename) {
        this.sevenZipFilename = sevenZipFilename;
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

    public Batch setItems(final List<Item> items) {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Batch{");
        sb.append("path='").append(path).append('\'');
        sb.append(", sevenZipFilename='").append(sevenZipFilename).append('\'');
        sb.append(", items=").append(items);
        sb.append('}');
        return sb.toString();
    }
}
