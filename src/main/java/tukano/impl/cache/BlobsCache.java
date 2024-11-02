package tukano.impl.cache;

import tukano.api.Result;

public interface BlobsCache {
    Result<Long> increaseBlobViews(String blobId);
    Result<Long> getLastUpdate(String blobId);

    Result<Void> updateLastUpdate(String blobId, long timestamp);

    Result<Long> getBlobViews(String blobId);

    Result<Void> setBlobViews(String blobId, long views);
}
