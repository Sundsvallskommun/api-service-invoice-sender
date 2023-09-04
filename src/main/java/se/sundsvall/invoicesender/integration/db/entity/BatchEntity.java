package se.sundsvall.invoicesender.integration.db.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "batch_executions")
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "total_invoices", nullable = false)
    private long totalItems;

    @Column(name = "sent_invoices", nullable = false)
    private long sentItems;

    public Integer getId() {
        return id;
    }

    public BatchEntity setId(final Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public BatchEntity setStartedAt(final LocalDateTime startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public BatchEntity setCompletedAt(final LocalDateTime completedAt) {
        this.completedAt = completedAt;
        return this;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public BatchEntity setTotalItems(final long totalItems) {
        this.totalItems = totalItems;
        return this;
    }

    public long getSentItems() {
        return sentItems;
    }

    public BatchEntity setSentItems(final long sentItems) {
        this.sentItems = sentItems;
        return this;
    }
}
