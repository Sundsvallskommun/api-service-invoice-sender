package se.sundsvall.invoicesender.integration.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    private Integer id;

    @OneToOne
    @MapsId
    private BatchEntity batch;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Lob
    @Column(name = "data", columnDefinition = "LONGBLOB NOT NULL")
    private byte[] data;

    public Integer getId() {
        return id;
    }

    public FileEntity withId(final Integer id) {
        this.id = id;
        return this;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public BatchEntity getBatch() {
        return batch;
    }

    public FileEntity withBatch(final BatchEntity batch) {
        this.batch = batch;
        return this;
    }

    public void setBatch(final BatchEntity batch) {
        this.batch = batch;
    }

    public String getFilename() {
        return filename;
    }

    public FileEntity withFilename(final String filename) {
        this.filename = filename;
        return this;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public byte[] getData() {
        return data;
    }

    public FileEntity withData(final byte[] data) {
        this.data = data;
        return this;
    }

    public void setData(final byte[] data) {
        this.data = data;
    }
}
