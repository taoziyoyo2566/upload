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

    public FileInfo(String name, String contentType, String path, Long size, String originalFilename) {
        this.name = name;
        this.contentType = contentType;
        this.path = path;
        this.size = size;
        this.originalFilename = originalFilename;
    }

    public FileInfo(String name, String originalFilename, String contentType, String path, Long size) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.path = path;
        this.size = size;
        this.uploadTime = LocalDateTime.now();
    }
}
