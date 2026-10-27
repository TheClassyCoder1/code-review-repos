package com.example.lending.loan.partner.docs;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Entity
@Table(name = "partner_documents", schema = "lending")
public class PartnerDocument {

    @Id
    private String id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "content_size")
    private long contentSize;

    @Column(name = "created_by")
    private Long createdBy;

    @Lob
    @Column(name = "uploaded")
    private byte[] uploaded;

    @Transient
    private MultipartFile uploadFile;

    public String generateId() {
        return UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getContentSize() { return contentSize; }
    public void setContentSize(long contentSize) { this.contentSize = contentSize; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public byte[] getUploaded() { return uploaded; }
    public void setUploaded(byte[] uploaded) { this.uploaded = uploaded; }
    public MultipartFile getUploadFile() { return uploadFile; }
    public void setUploadFile(MultipartFile uploadFile) { this.uploadFile = uploadFile; }
}
