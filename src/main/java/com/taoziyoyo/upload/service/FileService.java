package com.taoziyoyo.upload.service;


import com.taoziyoyo.upload.config.BackblazeConfig;
import com.taoziyoyo.upload.exception.FileStorageException;
import com.taoziyoyo.upload.model.FileInfo;
import com.taoziyoyo.upload.repository.FileRepository;
import jakarta.annotation.PostConstruct;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class FileService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.text-extensions:txt,md,html,css,js,java,py,c,cpp,h,json,xml,yml,yaml,properties,csv,log}")
    private String textExtensionsStr;

    @Value("${file.media-extensions:mp3,mp4,webm,ogg,wav,avi,mov,flv}")
    private String mediaExtensionsStr;

    private List<String> textExtensions;
    private List<String> mediaExtensions;

    private Path fileStoragePath;

    private final FileRepository fileRepository;

    private final BackblazeConfig backblazeConfig;

    private final BackblazeService backblazeService;

    private final Tika tika = new Tika();

    public FileService(FileRepository fileRepository, BackblazeConfig backblazeConfig, BackblazeService backblazeService) {
        this.fileRepository = fileRepository;
        this.backblazeConfig = backblazeConfig;
        this.backblazeService = backblazeService;
    }

    @PostConstruct
    public void init() {
        this.fileStoragePath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStoragePath);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored", ex);
        }

        this.textExtensions = Arrays.asList(textExtensionsStr.split(","));
        this.mediaExtensions = Arrays.asList(mediaExtensionsStr.split(","));
    }

    public FileInfo storeFile(MultipartFile file, boolean useCloudStorage) {
        // Normalize file name
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            // Check if the file's name contains invalid characters
            if(originalFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence " + originalFileName);
            }

            // Generate a unique file name
            String fileName = UUID.randomUUID() + "_" + originalFileName;

            if (useCloudStorage && backblazeConfig.isEnabled()) {
                // Upload to Back blaze B2
                String fileId = backblazeService.uploadFile(file);
                String downloadUrl = backblazeService.getDownloadUrl(fileId);

                // Save file info to database
                FileInfo fileInfo = new FileInfo(
                        fileName,
                        originalFileName,
                        file.getContentType(),
                        file.getSize(),
                        fileId,
                        downloadUrl
                );

                return fileRepository.save(fileInfo);
            } else {
                // Copy file to the target location (Replace existing file with the same name)
                Path targetLocation = this.fileStoragePath.resolve(fileName);
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

                // Save file info to database
                FileInfo fileInfo = new FileInfo(
                        fileName,
                        originalFileName,
                        file.getContentType(),
                        targetLocation.toString(),
                        file.getSize()
                );

                return fileRepository.save(fileInfo);
            }
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + originalFileName, ex);
        }
    }

    public Resource loadFileAsResource(FileInfo fileInfo) {
        try {
            if ("cloud".equals(fileInfo.getStorageType())) {
                // For cloud storage, we'll download the file first
                Path tempFile = Files.createTempFile("b2download_", fileInfo.getOriginalFilename());
                backblazeService.downloadFile(fileInfo.getCloudFileId(), tempFile);
                return new UrlResource(tempFile.toUri());
            } else {
                // For local storage
                Path filePath = Paths.get(fileInfo.getPath());
                Resource resource = new UrlResource(filePath.toUri());
                if(resource.exists()) {
                    return resource;
                } else {
                    throw new FileStorageException("File not found " + fileInfo.getName());
                }
            }
        } catch (IOException ex){
            throw new FileStorageException("File not found " + fileInfo.getName(), ex);
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
                if ("cloud".equals(fileInfo.getStorageType())) {
                    // Delete from cloud storage
                    backblazeService.deleteFile(fileInfo.getCloudFileId(), fileInfo.getName());
                } else {
                    // Delete from local filesystem
                    Path filePath = Paths.get(fileInfo.getPath());
                    Files.deleteIfExists(filePath);
                }
                fileRepository.delete(fileInfo);
            } catch (IOException e) {
                throw new FileStorageException("Could not delete file " + fileInfo.getName(), e);
            }
        }
    }

    public FileInfo updateFile(Long id, MultipartFile file, String newName) {
        Optional<FileInfo> fileInfoOpt = fileRepository.findById(id);
        if (fileInfoOpt.isPresent()) {
            FileInfo oldFileInfo = fileInfoOpt.get();
            boolean useCloudStorage = "cloud".equals(oldFileInfo.getStorageType());

            // Delete the old file
            deleteFile(id);

            // Store the new file
            FileInfo newFileInfo = storeFile(file, useCloudStorage);
            if (newName != null && !newName.isEmpty()) {
                newFileInfo.setName(newName);
                fileRepository.save(newFileInfo);
            }
            return newFileInfo;
        }
        return null;
    }

    public String getFileContent(FileInfo fileInfo) {
        try {
            Resource resource = loadFileAsResource(fileInfo);

            try (InputStream inputStream = resource.getInputStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new FileStorageException("Could not read file content", e);
        }
    }

    public void updateFileContent(FileInfo fileInfo, String content) {
        try {
            if ("cloud".equals(fileInfo.getStorageType())) {
                throw new FileStorageException("Editing cloud-stored files is not supported yet");
            }

            Path filePath = Paths.get(fileInfo.getPath());
            Files.writeString(filePath, content);
        } catch (IOException e) {
            throw new FileStorageException("Could not update file content", e);
        }
    }

    public boolean isTextFile(FileInfo fileInfo) {
        String filename = fileInfo.getOriginalFilename().toLowerCase();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);

        // Check if it's in the text extensions list
        if (textExtensions.contains(extension)) {
            return true;
        }

        // Use Tika for more accurate content type detection
        try {
            Resource resource = loadFileAsResource(fileInfo);
            String detectedType = tika.detect(resource.getInputStream());
            return detectedType.startsWith("text/") ||
                    detectedType.equals("application/json") ||
                    detectedType.equals("application/xml");
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isMediaFile(FileInfo fileInfo) {
        String filename = fileInfo.getOriginalFilename().toLowerCase();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);

        // Check if it's in the media extensions list
        if (mediaExtensions.contains(extension)) {
            return true;
        }

        // Use Tika for more accurate content type detection
        try {
            Resource resource = loadFileAsResource(fileInfo);
            String detectedType = tika.detect(resource.getInputStream());
            return detectedType.startsWith("audio/") || detectedType.startsWith("video/");
        } catch (IOException e) {
            return false;
        }
    }
}