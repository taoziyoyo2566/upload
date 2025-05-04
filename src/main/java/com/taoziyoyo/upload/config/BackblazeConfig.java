package com.taoziyoyo.upload.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Backblaze B2 Cloud Storage
 */
@Component
@ConfigurationProperties(prefix = "backblaze")
public class BackblazeConfig {

    private String applicationKeyId;
    private String applicationKey;
    private String bucketId;
    private String bucketName;
    private boolean enabled;

    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    public void setApplicationKeyId(String applicationKeyId) {
        this.applicationKeyId = applicationKeyId;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}