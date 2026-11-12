package com.bank.cims.service.dms;

import com.bank.cims.service.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DmsStorageService {

    private static final Logger log = LoggerFactory.getLogger(DmsStorageService.class);

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    @Qualifier("localObjectStoreProvider")
    private DmsStorageProvider localObjectStoreProvider;

    @Autowired
    @Qualifier("alfrescoDmsProvider")
    private DmsStorageProvider alfrescoDmsProvider;

    public DmsStorageProvider getActiveProvider() {
        String mode = configurationService.getDmsIntegrationMode();
        if ("Alfresco_DMS".equalsIgnoreCase(mode) || "Alfresco".equalsIgnoreCase(mode)) {
            return alfrescoDmsProvider;
        }
        return localObjectStoreProvider;
    }

    public String getActiveMode() {
        return getActiveProvider().getProviderName();
    }

    public DmsStoreResult storeDocument(String docId, String fileName, String contentType, byte[] content, Map<String, String> metadata) {
        DmsStorageProvider provider = getActiveProvider();
        log.info("Delegating document storage for docId={}, fileName={} to provider={}", docId, fileName, provider.getProviderName());
        return provider.storeDocument(docId, fileName, contentType, content, metadata);
    }

    public byte[] retrieveDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return null;
        // Check active provider first
        DmsStorageProvider provider = getActiveProvider();
        byte[] bytes = provider.retrieveDocument(dmsRef);
        if (bytes != null && bytes.length > 0) {
            return bytes;
        }
        // Fallback to the other provider in case document was stored under a previous mode
        if (provider == alfrescoDmsProvider) {
            return localObjectStoreProvider.retrieveDocument(dmsRef);
        } else {
            return alfrescoDmsProvider.retrieveDocument(dmsRef);
        }
    }

    public boolean deleteDocument(String dmsRef) {
        if (dmsRef == null || dmsRef.isBlank()) return false;
        boolean deleted = getActiveProvider().deleteDocument(dmsRef);
        if (!deleted) {
            deleted = localObjectStoreProvider.deleteDocument(dmsRef) || alfrescoDmsProvider.deleteDocument(dmsRef);
        }
        return deleted;
    }

    public boolean isHealthy() {
        return getActiveProvider().isAvailable();
    }
}
