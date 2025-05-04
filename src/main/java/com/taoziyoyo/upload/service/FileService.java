package com.taoziyoyo.upload.service;


import com.taoziyoyo.upload.exception.FileStorageException;
import com.taoziyoyo.upload.model.FileInfo;
import com.taoziyoyo.upload.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStoragePath;

    @Autowired
    private FileRepository fileRepository;

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored", ex);
        }
    }

    public FileInfo storeFile(MultipartFile file) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            // Check if the file's name contains invalid characters
            if (originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence " + originalFileName);
            }

            // Generate a unique file name
            String fileName = UUID.randomUUID() + "_" + originalFileName;

            // Copy file to the target location
            Path targetLocation = this.fileStoragePath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Save file info to database
            FileInfo fileInfo = new FileInfo(
                    fileName,
                    originalFileName,
                    file.getContentType(),
                    file.getSize(),
                    targetLocation.toString()
            );

            return fileRepository.save(fileInfo);

        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.fileStoragePath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("File not found " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new FileStorageException("File not found " + fileName, ex);
        }
    }

    public List<FileInfo> getAllFiles() {
        return fileRepository.findAll();
    }

    public Optional<FileInfo> getFile(Long id) {
        return fileRepository.findById(id);
    }

    public void deleteFile(Long id) {
        Optional<FileInfo> fileInfoOpt = fileRepository.findById(id);
        if (fileInfoOpt.isPresent()) {
            FileInfo fileInfo = fileInfoOpt.get();
            try {
                Path filePath = Paths.get(fileInfo.getPath());
                Files.deleteIfExists(filePath);
                fileRepository.delete(fileInfo);
            } catch (IOException e) {
                throw new FileStorageException("Could not delete file " + fileInfo.getName(), e);
            }
        }
    }

    public FileInfo updateFile(Long id, MultipartFile file, String newName) {
        Optional<FileInfo> fileInfoOpt = fileRepository.findById(id);
        if (fileInfoOpt.isPresent()) {
            // Delete the old file
            deleteFile(id);
            // Store the new file
            FileInfo newFileInfo = storeFile(file);
            if (newName != null && !newName.isEmpty()) {
                newFileInfo.setName(newName);
                fileRepository.save(newFileInfo);
            }
            return newFileInfo;
        }
        return null;
    }
}