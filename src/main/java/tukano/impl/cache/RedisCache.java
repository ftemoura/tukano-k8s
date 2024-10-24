package tukano.impl.cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.params.SetParams;
import tukano.api.Result;
import utils.ConfigLoader;
import utils.Pair;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static tukano.api.Result.ErrorCode.*;

public class RedisCache {

    private static final boolean CACHE_ENABLED = ConfigLoader.getInstance().isCacheEnabled();
    private static final int REDIS_PORT = 6380;
    private static final int REDIS_TIMEOUT = 2000;
    private static final boolean REDIS_USE_TLS = true;

    private static JedisPool instance;

    public synchronized static JedisPool getCachePool() {
        if( instance != null)
            return instance;
        String redisHostname = ConfigLoader.getInstance().getRedisHostname();
        String redisKey = ConfigLoader.getInstance().getRedisKey();
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        instance = new JedisPool(poolConfig, redisHostname, REDIS_PORT, REDIS_TIMEOUT, redisKey, REDIS_USE_TLS);
        return instance;
    }

    private <T> Result<T> cache(Function<Void,Result<T>> f) {
        if (CACHE_ENABLED)
            return f.apply(null);
        return Result.error(NOT_FOUND);

    }

    protected  <T> Result<T> execute(Jedis jedis, Function<Transaction, Result<T>> func) {
        try {
            Transaction transaction = jedis.multi();
            Result<T> result = func.apply(transaction);
            List<Object> results = transaction.exec();
            if (results == null) {
                return Result.error(CONFLICT);
            }
            return result;
        }catch (RedisTimestampTransactionException e) {
            return Result.error(FORBIDDEN); //TODO MUDAR
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(CONFLICT);
        }
    }

    protected Result<Void> setKeyValue(String key, String value, LocalDateTime timestamp, int ttl) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                jedis.watch(key);
                String existingValueWithTimestamp = jedis.get(key);
                String newValueWithTimestamp = value + "," + timestamp.toString();
                if (existingValueWithTimestamp != null) {
                    String[] parts = existingValueWithTimestamp.split(",");
                    LocalDateTime existingTimestamp = LocalDateTime.parse(parts[1]);

                    if (!timestamp.isAfter(existingTimestamp))
                        throw new RedisTimestampTransactionException("Conflict: New timestamp is not after the current timestamp.");
                }
                return execute(jedis, transaction -> {
                    transaction.set(key, newValueWithTimestamp, SetParams.setParams().ex(ttl));
                    return Result.ok();
                });
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
            });
    }

    protected Result<String> getKeyValue(String key) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                String valueWithTimestamp = jedis.get(key);
                if (valueWithTimestamp == null) {
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }
                String[] parts = valueWithTimestamp.split(",");
                String value = parts[0];
                //Instant timestamp = Instant.parse(parts[1]);
                return Result.ok(value);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        });
    }

    protected Result<Void> deleteKey(String key) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                jedis.del(key);
                return Result.ok();
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        });
    }

    /*private void checkTimestamp(String key, Instant timestamp, Jedis jedis) {
        jedis.watch(key, key + ":meta");
        String currentTimestampString = jedis.hget(key + ":meta", "timestamp");

        if (currentTimestampString != null) {
            Instant currentTimestamp = Instant.parse(currentTimestampString);
            if (!timestamp.isAfter(currentTimestamp)) {
                throw new RedisTimestampTransactionException("Conflict: New timestamp is not after the current timestamp.");
            }
        }
    }*/

    protected Result<Void> addToSet(String key, String value) {
        return cache(__ -> {
                    try (Jedis jedis = getCachePool().getResource()) {
                        jedis.sadd(key, value);
                        return Result.ok();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Result.error(INTERNAL_ERROR);
                    }
                });
        /*try (Jedis jedis = getCachePool().getResource()) {
            checkTimestamp(key, timestamp, jedis);

            return execute(jedis, transaction -> {
                transaction.sadd(key, value);
                transaction.hset(key + ":meta", "timestamp", timestamp.toString());
                return Result.ok();
            });
        } catch (Exception e) {
            return Result.error(INTERNAL_ERROR);
        }*/
    }

    protected Result<String> removeFromSet(String key, String value) {
        return cache(__ -> {
                    try (Jedis jedis = getCachePool().getResource()) {
                        jedis.srem(key, value);
                        return Result.ok(value);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Result.error(INTERNAL_ERROR);
                    }
                });
        /*try (Jedis jedis = getCachePool().getResource()) {
            checkTimestamp(key, timestamp, jedis);

            return execute(jedis, transaction -> {
                transaction.srem(key, value);
                transaction.hset(key + ":meta", "timestamp", timestamp.toString());
                return Result.ok(value);
            });
        } catch (Exception e) {
            return Result.error(INTERNAL_ERROR);
        }*/
    }

    protected Result<Set<String>> getSetMembers(String key) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                Set<String> members = jedis.smembers(key);
                if (members.isEmpty()) {
                    return Result.error(Result.ErrorCode.NOT_FOUND);
                }

                //String timestampString = jedis.hget(key + ":meta", "timestamp");
                //Instant timestamp = Instant.parse(timestampString);
                return Result.ok(members);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        });
    }

    protected Result<Pair<Long, LocalDateTime>> incrementCounter(String key) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                long value = jedis.incr(key);
                return Result.ok(new Pair<>(value, LocalDateTime.now()));
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        });
    }

    protected Result<Pair<Long, LocalDateTime>> decrementCounter(String key) {
        return cache(__ -> {
            try (Jedis jedis = getCachePool().getResource()) {
                long value = jedis.decr(key);
                return Result.ok(new Pair<>(value, LocalDateTime.now()));
            } catch (Exception e) {
                e.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        });
    }

    // We use the exception to cancel the transaction
    private static class RedisTimestampTransactionException extends RuntimeException {
        public RedisTimestampTransactionException(String message) {
            super(message);
        }
    }
}
