package com.taoziyoyo.upload.service;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.contentHandlers.B2ContentMemoryWriter;
import com.backblaze.b2.client.contentHandlers.B2ContentOutputStreamWriter;
import com.backblaze.b2.client.contentSources.B2ContentSource;
import com.backblaze.b2.client.contentSources.B2ContentTypes;
import com.backblaze.b2.client.contentSources.B2FileContentSource;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.backblaze.b2.client.structures.B2UploadFileRequest;
import com.backblaze.b2.client.contentHandlers.B2ContentOutputStreamWriter;
import com.taoziyoyo.upload.config.BackblazeConfig;
import com.taoziyoyo.upload.exception.FileStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PreDestroy;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BackblazeService {

    private static final Logger logger = LoggerFactory.getLogger(BackblazeService.class);

    @Autowired
    private BackblazeConfig backblazeConfig;

    private B2StorageClient client;

    /**
     * Initialize the B2 client
     */
    private void initializeClient() {
        if (client == null && backblazeConfig.isEnabled()) {
            client = B2StorageClientFactory
                    .createDefaultFactory()
                    .create(backblazeConfig.getApplicationKeyId(),
                            backblazeConfig.getApplicationKey(),
                            "FileUploadApp");
            logger.info("Backblaze B2 client initialized");
        }
    }

    /**
     * Upload a file to Backblaze B2
     * @param file The MultipartFile to upload
     * @return The file ID in B2
     */
    public String uploadFile(MultipartFile file) {
        if (!backblazeConfig.isEnabled()) {
            throw new FileStorageException("Backblaze B2 storage is not enabled");
        }

        initializeClient();

        try {
            // First save the file to a temporary location
            File tempFile = convertMultiPartToFile(file);

            // Generate a unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

            // Prepare metadata
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("originalFileName", file.getOriginalFilename());
            fileInfo.put("contentType", file.getContentType());

            // Create the upload request
            B2ContentSource contentSource = B2FileContentSource.builder(tempFile).build();
            B2UploadFileRequest request = B2UploadFileRequest
                    .builder(backblazeConfig.getBucketId(), fileName,
                            B2ContentTypes.B2_AUTO, contentSource)
                    .setCustomFields(fileInfo)
                    .build();

            // Upload the file
            B2FileVersion fileVersion = client.uploadSmallFile(request);

            // Clean up the temp file
            Files.deleteIfExists(tempFile.toPath());

            return fileVersion.getFileId();
        } catch (B2Exception | IOException e) {
            logger.error("Error uploading file to Backblaze B2", e);
            throw new FileStorageException("Error uploading file to cloud storage", e);
        }
    }

    /**
     * Download a file from Backblaze B2
     * @param fileId The file ID in B2
     * @return The downloaded file as a byte array
     */
    public byte[] downloadFile(String fileId) {
        if (!backblazeConfig.isEnabled()) {
            throw new FileStorageException("Backblaze B2 storage is not enabled");
        }

        initializeClient();

        try {
            B2ContentMemoryWriter contentHandler = B2ContentMemoryWriter.build();
            client.downloadById(fileId, contentHandler);
            return contentHandler.getBytes();
        } catch (B2Exception e) {
            logger.error("Error downloading file from Backblaze B2", e);
            throw new FileStorageException("Error downloading file from cloud storage", e);
        }
    }

    /**
     * Download a file from Backblaze B2 to a local file
     * @param fileId The file ID in B2
     * @param targetPath The path to save the file to
     */
    public void downloadFile(String fileId, Path targetPath) throws FileNotFoundException {
        if (!backblazeConfig.isEnabled()) {
            throw new FileStorageException("Backblaze B2 storage is not enabled");
        }

        initializeClient();

        try (OutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
            B2ContentMemoryWriter contentHandler = B2ContentMemoryWriter.builder().build();
//            B2ContentOutputStreamWriter contentHandler = B2ContentOutputStreamWriter.build(outputStream);
            client.downloadById(fileId, contentHandler);
        } catch (B2Exception | IOException e) {
            logger.error("Error downloading file from Backblaze B2", e);
            throw new FileStorageException("Error downloading file from cloud storage", e);
        }
    }

    /**
     * Delete a file from Backblaze B2
     * @param fileId The file ID in B2
     * @param fileName The file name in B2
     */
    public void deleteFile(String fileId, String fileName) {
        if (!backblazeConfig.isEnabled()) {
            throw new FileStorageException("Backblaze B2 storage is not enabled");
        }

        initializeClient();

        try {
            client.deleteFileVersion(fileName, fileId);
        } catch (B2Exception e) {
            logger.error("Error deleting file from Backblaze B2", e);
            throw new FileStorageException("Error deleting file from cloud storage", e);
        }
    }

    /**
     * Get the download URL for a file
     * @param fileId The file ID in B2
     * @return The download URL
     */
    public String getDownloadUrl(String fileId) {
        if (!backblazeConfig.isEnabled()) {
            throw new FileStorageException("Backblaze B2 storage is not enabled");
        }

        initializeClient();

        try {
            return client.getDownloadByIdUrl(fileId);
        } catch (B2Exception e) {
            logger.error("Error getting download URL from Backblaze B2", e);
            throw new FileStorageException("Error getting download URL from cloud storage", e);
        }
    }

    /**
     * Convert MultipartFile to File
     * @param file The MultipartFile to convert
     * @return The converted File
     * @throws IOException If an I/O error occurs
     */
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("b2upload_", "_" + file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        return tempFile;
    }

    /**
     * Close the client when the service is destroyed
     */
    @PreDestroy
    public void closeClient() {
        if (client != null) {
            client.close();
        }
    }
}