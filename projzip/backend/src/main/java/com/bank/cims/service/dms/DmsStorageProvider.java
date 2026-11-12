package com.bank.cims.service.dms;

import java.util.Map;

public interface DmsStorageProvider {
    String getProviderName();
    DmsStoreResult storeDocument(String docId, String fileName, String contentType, byte[] content, Map<String, String> metadata);
    byte[] retrieveDocument(String dmsRef);
    boolean deleteDocument(String dmsRef);
    boolean isAvailable();
}
