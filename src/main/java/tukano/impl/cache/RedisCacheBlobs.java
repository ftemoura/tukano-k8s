package tukano.impl.cache;

import tukano.api.Result;

public class RedisCacheBlobs extends RedisCache implements BlobsCache {

    private static final String BLOB_VIEWS = "blob_views:";
    private static final String BLOB_VIEWS_LAST_UPDATE = "blob_views_last_update:";

    @Override
    public  Result<Long> increaseBlobViews(String blobId) {
        String cacheKey = BLOB_VIEWS + blobId;
        Result<Long> res = super.incrementCounter(cacheKey);
        return res;
    }

    @Override
    public Result<Long> getLastUpdate(String blobId) {
        String cacheKey = BLOB_VIEWS_LAST_UPDATE + blobId;
        Result<String> res = super.getKeyValueWithoutTimestamp(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(Long.parseLong(res.value()));
    }

    @Override
    public Result<Void> updateLastUpdate(String blobId, long timestamp) {
        String cacheKey = BLOB_VIEWS_LAST_UPDATE + blobId;
        Result<Void> res = super.setKeyValue(cacheKey, Long.toString(timestamp));
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok();
    }

    @Override
    public Result<Long> getBlobViews(String blobId) {
        String cacheKey = BLOB_VIEWS + blobId;
        Result<String> res = super.getKeyValueWithoutTimestamp(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(Long.parseLong(res.value()));
    }

    @Override
    public Result<Void> setBlobViews(String blobId, long views) {
        String cacheKey = BLOB_VIEWS + blobId;
        super.setKeyValue(cacheKey, Long.toString(views));
        return Result.ok();
    }

}
