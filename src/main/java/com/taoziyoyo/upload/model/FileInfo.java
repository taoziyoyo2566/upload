package com.taoziyoyo.upload.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String originalFilename;

    private String contentType;

    private String path;

    private Long size;

    private LocalDateTime uploadTime;

    private String storageType; // "local" or "cloud"

    private String cloudFileId; // For cloud storage file ID

    private String downloadUrl; // For cloud storage download URL


    public FileInfo(String name, String originalFilename, String contentType, String path, Long size) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.path = path;
        this.size = size;
        this.uploadTime = LocalDateTime.now();
        this.storageType = "local";
    }

    public FileInfo(String name, String originalFilename, String contentType, Long size, String cloudFileId, String downloadUrl) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.uploadTime = LocalDateTime.now();
        this.storageType = "cloud";
        this.cloudFileId = cloudFileId;
        this.downloadUrl = downloadUrl;
    }
}
