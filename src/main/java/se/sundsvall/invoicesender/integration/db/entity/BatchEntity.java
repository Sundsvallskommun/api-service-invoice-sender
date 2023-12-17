package se.sundsvall.invoicesender.integration.db.entity;

import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.FetchType.EAGER;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_executions")
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "basename", nullable = false)
    private String basename;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @OneToMany(fetch = EAGER, cascade = PERSIST)
    @JoinColumn(name = "batch_id", nullable = false)
    private List<ItemEntity> items;

    @Column(name = "total_invoices", nullable = false)
    private long totalItems;

    @Column(name = "sent_invoices", nullable = false)
    private long sentItems;

    public Integer getId() {
        return id;
    }

    public BatchEntity withId(final Integer id) {
        this.id = id;
        return this;
    }

    public String getBasename() {
        return basename;
    }

    public BatchEntity withBasename(final String basename) {
        this.basename = basename;
        return this;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public BatchEntity withStartedAt(final LocalDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public BatchEntity withCompletedAt(final LocalDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public List<ItemEntity> getItems() {
        return items;
    }

    public BatchEntity withItems(final List<ItemEntity> items) {
        this.items = items;
        return this;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public BatchEntity withTotalItems(final long totalItems) {
        this.totalItems = totalItems;
        return this;
    }

    public long getSentItems() {
        return sentItems;
    }

    public BatchEntity withSentItems(final long sentItems) {
        this.sentItems = sentItems;
        return this;
    }
}
