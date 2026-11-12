package com.bank.cims.service.dms;

import java.time.LocalDateTime;
import java.util.Map;

public class DmsStoreResult {
    private final String dmsRef;
    private final String repositoryMode;
    private final String fileName;
    private final long fileSize;
    private final LocalDateTime storedAt;
    private final boolean success;
    private final String message;

    public DmsStoreResult(String dmsRef, String repositoryMode, String fileName, long fileSize, boolean success, String message) {
        this.dmsRef = dmsRef;
        this.repositoryMode = repositoryMode;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.storedAt = LocalDateTime.now();
        this.success = success;
        this.message = message;
    }

    public String getDmsRef() { return dmsRef; }
    public String getRepositoryMode() { return repositoryMode; }
    public String getFileName() { return fileName; }
    public long getFileSize() { return fileSize; }
    public LocalDateTime getStoredAt() { return storedAt; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
