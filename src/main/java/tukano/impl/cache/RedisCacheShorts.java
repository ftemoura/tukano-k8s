package tukano.impl.cache;

import com.sun.xml.xsom.impl.scd.Step;
import tukano.api.Result;
import tukano.api.Short;
import utils.JSON;
import utils.Pair;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class RedisCacheShorts extends RedisCache implements ShortsCache {
    //private static final String
    private static final int SHORTS_TTL = 3600;

    private static final String SHORT_KEY = "short:";
    private static final String USERS_SHORTS_KEY = "userShorts:";
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
    public Result<Void> cacheShorts(String userId) {
        return null;
    }
}
