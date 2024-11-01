package tukano.impl.cache;

import com.sun.xml.xsom.impl.scd.Step;
import tukano.api.Result;
import tukano.api.Short;
import utils.JSON;
import utils.Pair;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static java.lang.String.format;

public class RedisCacheShorts extends RedisCache implements ShortsCache {
    //private static final String
    private static final int SHORTS_TTL = 3600;

    private static final int USERS_SHORTS_TTL = 600;
    private static final int USER_FOLLOWERS_TTL = 600;
    private static final int SHORT_LIKES_TTL = 600;
    private static final int USER_FEED_TTL = 10;

    private static final String SHORT_KEY = "short:";
    private static final String USERS_SHORTS_KEY = "userShorts:";
    private static final String USER_FOLLOWERS_KEY = "followers:";
    private static final String SHORT_LIKES_KEY = "likes:";
    private static final String USER_FEED_KEY = "feed:";
    @Override
    public Result<Void> cacheShort(Short shrt, LocalDateTime timestamp) {
        String cacheKey = SHORT_KEY + shrt.getShortId();
        return super.setKeyValue(cacheKey, JSON.encode(shrt), timestamp, SHORTS_TTL);
    }

    @Override
    public Result<Void> deleteShort(String shortId) {
        String cacheKey = SHORT_KEY + shortId;
        return super.deleteKey(cacheKey);
    }

    @Override
    public Result<Short> getShort(String shortId) {
        String cacheKey = SHORT_KEY + shortId;
        Result<String> res = super.getKeyValue(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(JSON.decode(res.value(), Short.class));
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        String cacheKey = USERS_SHORTS_KEY + userId;
        var res = super.getSetMembers(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(res.value().stream().toList());
    }

    @Override
    public Result<Void> cacheShorts(String userId, List<String> shortIds) {
        String cacheKey = USERS_SHORTS_KEY + userId;
        return super.createSet(cacheKey, USERS_SHORTS_TTL, shortIds);
    }

    @Override
    public Result<List<String>> getFollowers(String userId) {
        String cacheKey = USER_FOLLOWERS_KEY + userId;
        var res = super.getSetMembers(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(res.value().stream().toList());
    }

    @Override
    public Result<Void> cacheFollowers(String userId, List<String> followers) {
        String cacheKey = USER_FOLLOWERS_KEY + userId;
        return super.createSet(cacheKey, USER_FOLLOWERS_TTL, followers);
    }

    @Override
    public Result<List<String>> getLikes(String shortId) {
        String cacheKey = SHORT_LIKES_KEY + shortId;
        var res = super.getSetMembers(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(res.value().stream().toList());
    }

    @Override
    public Result<Void> cacheLikes(String shortId, List<String> likes) {
        String cacheKey = SHORT_LIKES_KEY + shortId;
        return super.createSet(cacheKey, SHORT_LIKES_TTL, likes);
    }

    @Override
    public Result<List<String>> getFeed(String userId) {
        String cacheKey = USER_FEED_KEY + userId;
        var res = super.getSetMembers(cacheKey);
        if (!res.isOK())
            return Result.error(res.error());
        return Result.ok(res.value().stream().toList());
    }

    @Override
    public Result<Void> cacheFeed(String userId, List<String> feed) {
        String cacheKey = USER_FEED_KEY + userId;
        return super.createSet(cacheKey, USER_FEED_TTL, feed);
    }

    @Override
    public Result<Void> invalidateLikes(String shortId) {
        String cacheKey = SHORT_LIKES_KEY + shortId;
        return super.deleteKey(cacheKey);
    }

    @Override
    public Result<Void> invalidateFollowers(String userId) {
        String cacheKey = USER_FOLLOWERS_KEY + userId;
        return super.deleteKey(cacheKey);
    }

    @Override
    public Result<Void> invalidateFeed(String userId) {
        String cacheKey = USER_FEED_KEY + userId;
        return super.deleteKey(cacheKey);
    }

}
