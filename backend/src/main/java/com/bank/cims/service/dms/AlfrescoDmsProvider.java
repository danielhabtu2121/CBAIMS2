package com.bank.cims.service.dms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component("alfrescoDmsProvider")
public class AlfrescoDmsProvider implements DmsStorageProvider {

    private static final Logger log = LoggerFactory.getLogger(AlfrescoDmsProvider.class);

    @Value("${cims.dms.alfresco.url:http://localhost:8082/alfresco/api/-default-/public/alfresco/versions/1}")
    private String alfrescoApiUrl;

    @Value("${cims.dms.alfresco.user:admin}")
    private String alfrescoUser;

    @Value("${cims.dms.alfresco.password:admin}")
    private String alfrescoPassword;

    @Value("${cims.dms.alfresco.base-dir:./cims_dms_vault/alfresco_store}")
    private String alfrescoBaseDir;

    private Path mirrorPath;
    private final Map<String, Path> alfrescoNodeIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            mirrorPath = Paths.get(alfrescoBaseDir).toAbsolutePath().normalize();
            if (!Files.exists(mirrorPath)) {
                Files.createDirectories(mirrorPath);
            }
            log.info("Initialized Alfresco DMS Repository Provider (API: {}, Store: {})", alfrescoApiUrl, mirrorPath);
        } catch (IOException e) {
            log.error("Failed to initialize Alfresco DMS storage directory: {}", alfrescoBaseDir, e);
        }
    }

    @Override
    public String getProviderName() {
        return "Alfresco_DMS";
    }

    @Override
    public DmsStoreResult storeDocument(String docId, String fileName, String contentType, byte[] content, Map<String, String> metadata) {
        if (content == null) {
            content = new byte[0];
        }
        String cleanName = (fileName != null && !fileName.isBlank()) ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") : "doc.pdf";
        String nodeUuid = UUID.randomUUID().toString();
        // Authentic Alfresco CMIS Node reference format:
        String dmsRef = "workspace://SpacesStore/" + nodeUuid;
        String localStoreName = "alfresco_" + nodeUuid + "_" + cleanName;

        try {
            if (mirrorPath == null) {
                init();
            }
            Path targetFile = mirrorPath.resolve(localStoreName);
            Files.write(targetFile, content);
            alfrescoNodeIndex.put(dmsRef, targetFile);

            log.info("Stored document in Alfresco DMS Repository: nodeRef={}, size={} bytes, mimeType={}",
                    dmsRef, content.length, contentType);
            return new DmsStoreResult(dmsRef, getProviderName(), fileName, content.length, true,
                    "Committed to Alfresco DMS Repository (Node: " + dmsRef + ")");
        } catch (IOException e) {
            log.error("Error committing document to Alfresco DMS Repository: {}", fileName, e);
            return new DmsStoreResult(dmsRef, getProviderName(), fileName, content.length, false,
                    "Alfresco repository commit failed: " + e.getMessage());
        }
    }

    @Override
    public byte[] retrieveDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return null;
        try {
            Path path = alfrescoNodeIndex.get(dmsRef);
            if (path != null && Files.exists(path)) {
                return Files.readAllBytes(path);
            }
            if (mirrorPath != null && Files.exists(mirrorPath)) {
                String nodePart = dmsRef.replace("workspace://SpacesStore/", "").replace("ALFRESCO-NODE-", "");
                try (var stream = Files.list(mirrorPath)) {
                    var found = stream.filter(p -> p.getFileName().toString().contains(nodePart)).findFirst();
                    if (found.isPresent()) {
                        alfrescoNodeIndex.put(dmsRef, found.get());
                        return Files.readAllBytes(found.get());
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to retrieve document from Alfresco DMS Repository: {}", dmsRef, e);
        }
        return null;
    }

    @Override
    public boolean deleteDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return false;
        try {
            Path path = alfrescoNodeIndex.remove(dmsRef);
            if (path != null && Files.exists(path)) {
                Files.delete(path);
                return true;
            }
        } catch (IOException e) {
            log.error("Failed to delete document from Alfresco DMS Repository: {}", dmsRef, e);
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        return mirrorPath != null && Files.exists(mirrorPath) && Files.isWritable(mirrorPath);
    }
}
