package tukano.impl.cache;

import tukano.api.Result;
import tukano.api.Short;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface ShortsCache {
    Result<Void> cacheShort(Short shrt, LocalDateTime timestamp);

    Result<Void> deleteShort(String shortId);

    Result<Short> getShort(String shortId);

    Result<List<String>> getShorts(String userId);

    Result<Void> cacheShorts(String userId);

}
