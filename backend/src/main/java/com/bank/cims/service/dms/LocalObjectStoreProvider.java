package com.bank.cims.service.dms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("localObjectStoreProvider")
public class LocalObjectStoreProvider implements DmsStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalObjectStoreProvider.class);

    @Value("${cims.dms.local.base-dir:./cims_dms_vault}")
    private String baseDir;

    private Path storagePath;
    private final Map<String, Path> refPathIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            storagePath = Paths.get(baseDir).toAbsolutePath().normalize();
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            log.info("Initialized Local Object Store DMS at: {}", storagePath);
        } catch (IOException e) {
            log.error("Failed to initialize Local Object Store directory: {}", baseDir, e);
        }
    }

    @Override
    public String getProviderName() {
        return "Local_Object_Store";
    }

    @Override
    public DmsStoreResult storeDocument(String docId, String fileName, String contentType, byte[] content, Map<String, String> metadata) {
        if (content == null) {
            content = new byte[0];
        }
        String cleanName = (fileName != null && !fileName.isBlank()) ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "doc.bin";
        String uuid = UUID.randomUUID().toString();
        String dmsRef = "LOCAL-OBJ-" + uuid;
        String storedFileName = docId + "_" + uuid.substring(0, 8) + "_" + cleanName;

        try {
            if (storagePath == null) {
                init();
            }
            Path targetFile = storagePath.resolve(storedFileName);
            Files.write(targetFile, content);
            refPathIndex.put(dmsRef, targetFile);

            log.info("Stored document in Local Object Store: ref={}, path={}, size={} bytes", dmsRef, targetFile, content.length);
            return new DmsStoreResult(dmsRef, getProviderName(), fileName, content.length, true, "Stored in Local Object Store");
        } catch (IOException e) {
            log.error("Error storing document in Local Object Store: {}", fileName, e);
            return new DmsStoreResult(dmsRef, getProviderName(), fileName, content.length, false, "Local storage failed: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieveDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return null;
        try {
            Path path = refPathIndex.get(dmsRef);
            if (path != null && Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            // Check storage directory by pattern
            if (storagePath != null && Files.exists(storagePath)) {
                try (var stream = Files.list(storagePath)) {
                    var found = stream.filter(p -> p.getFileName().toString().contains(dmsRef.replace("LOCAL-OBJ-", "")))
                            .findFirst();
                    if (found.isPresent()) {
                        refPathIndex.put(dmsRef, found.get());
                        return Files.readAllBytes(found.get());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to retrieve document from Local Object Store: {}", dmsRef, e);
        }
        return null;
    }

    @Override
    public boolean deleteDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return false;
        try {
            Path path = refPathIndex.remove(dmsRef);
            if (path != null && Files.exists(path)) {
                Files.delete(path);
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to delete document from Local Object Store: {}", dmsRef, e);
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return storagePath != null && Files.exists(storagePath) && Files.isWritable(storagePath);
    }
}
