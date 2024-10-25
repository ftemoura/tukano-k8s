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

    Result<Void> cacheShorts(String userId, List<String> shortIds);

    Result<List<String>> getFollowers(String userId);

    Result<Void> cacheFollowers(String userId, List<String> followers);

    Result<List<String>> getLikes(String shortId);

    Result<Void> cacheLikes(String shortId, List<String> likes);

    Result<List<String>> getFeed(String userId);

    Result<Void> cacheFeed(String userId, List<String> feed);

    Result<Void> invalidateLikes(String shortId);

    Result<Void> invalidateFollowers(String userId);

    Result<Void> invalidateFeed(String userId);
}
