package test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import tukano.api.Result;
import tukano.impl.cache.RedisCache;
import utils.Pair;

import java.time.Instant;
import java.util.Set;

public class RedisCacheTest {
    private RedisCache redisCache;
    private JedisPool pool;

    public void setUp() {
        redisCache = new RedisCache();
        pool = RedisCache.getCachePool();
    }

    public void tearDown() {
        try (Jedis jedis = pool.getResource()) {
            jedis.flushDB(); // Clear the Redis database after each test
        }
    }

    public void testAddToSet() {
        Instant timestamp = Instant.now();
        Result<Void> result = redisCache.addToSet("testSet", "value1", timestamp);

        assert result.isOK() : "Should successfully add value to set";
        //assert "value1".equals(result.value().getFirst()) : "Returned value should be 'value1'";
        //assert timestamp.equals(result.value().getSecond()) : "Timestamp should match";
    }

    public void testGetSetMembers() {
        Instant timestamp = Instant.now();
        redisCache.addToSet("testSet", "value1", timestamp);
        Result<Void> res = redisCache.addToSet("testSet", "value2", timestamp);
        if(!res.isOK())
            System.out.println(res.error());

        Result<Set<String>> result = redisCache.getSetMembers("testSet");

        assert result.isOK() : "Should successfully retrieve members of the set";
        assert result.value().contains("value1") : "Set should contain 'value1'";
        assert result.value().contains("value2") : "Set should contain 'value2'";
        //assert timestamp.equals(result.value().getSecond()) : "Timestamp should match";
    }

    public void testRemoveFromSet() {
        Instant timestamp = Instant.now();
        redisCache.addToSet("testSet", "value1", timestamp);
        Result<String> removeResult = redisCache.removeFromSet("testSet", "value1");

        assert removeResult.isOK() : "Should successfully remove value from set";
        assert "value1".equals(removeResult.value()) : "Removed value should be 'value1'";
    }

    public void testSetKeyValue() {
        Instant timestamp = Instant.now();
        Result<Void> result = redisCache.setKeyValue("testKey", "testValue", timestamp);

        assert result.isOK() : "Should successfully set key-value pair";
        //assert result.value().getFirst() == null : "No previous value should exist";

        // Verify the value
        Result<Pair<String, Instant>> getResult = redisCache.getKeyValue("testKey");
        assert getResult.isOK() : "Should successfully get key-value pair";
        assert "testValue".equals(getResult.value().getFirst()) : "Value should be 'testValue'";
        assert timestamp.equals(getResult.value().getSecond()) : "Timestamp should match";
    }

    public void testIncrementCounter() {
        Result<Pair<Long, Instant>> incrementResult = redisCache.incrementCounter("testCounter");
        assert incrementResult.isOK() : "Should successfully increment counter";
        assert incrementResult.value().getFirst() == 1L : "Counter should be 1";

        // Increment again
        Result<Pair<Long, Instant>> incrementResult2 = redisCache.incrementCounter("testCounter");
        assert incrementResult2.value().getFirst() == 2L : "Counter should be 2";
    }

    public void testDecrementCounter() {
        redisCache.incrementCounter("testCounter"); // Ensure counter starts at 1
        Result<Pair<Long, Instant>> decrementResult = redisCache.decrementCounter("testCounter");
        assert decrementResult.isOK() : "Should successfully decrement counter";
        assert decrementResult.value().getFirst() == 0L : "Counter should be 0 after decrementing";
    }

    public static void main(String[] args) {
        RedisCacheTest test = new RedisCacheTest();

        test.setUp();
        try {
            test.testAddToSet();
            test.testGetSetMembers();
            test.testRemoveFromSet();
            test.testSetKeyValue();
            test.testIncrementCounter();
            test.testDecrementCounter();
        } finally {
            test.tearDown();
        }
    }
}
