package com.taskflow.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * REDIS CONFIGURATION — Sets up caching with Redis.
 *
 * ============================================================
 * WHAT IS REDIS?
 * ============================================================
 *
 * Redis (Remote Dictionary Server) is an in-memory key-value database.
 * Created in 2009 by Salvatore Sanfilippo. Used by Twitter, GitHub, Stack Overflow,
 * Instagram, Airbnb, and basically every major tech company.
 *
 * "In-memory" means data lives in RAM, not on disk.
 *
 *   PostgreSQL (disk):   ~5-20 ms per query
 *   Redis (RAM):         ~0.1-1 ms per lookup
 *   That's 10-100x faster.
 *
 * Redis supports multiple data structures:
 *   String  → simple key-value (what we use for caching)
 *   List    → ordered collection (message queues)
 *   Set     → unique values (unique visitors)
 *   Hash    → field-value pairs (like a Java Map)
 *   Sorted Set → leaderboards (users ranked by score)
 *   TTL     → auto-expiry ("delete this key after 10 minutes")
 *
 * ============================================================
 * WHY CACHE?
 * ============================================================
 *
 * Problem: Some data is READ much more often than it CHANGES.
 *   - "GET /api/projects/1" might be called 1000 times per minute
 *   - The project data changes maybe once per day
 *   - Why hit the database 1000 times for the same data?
 *
 * Solution: Cache the result after the first DB query.
 *   Request 1:  DB query → 15ms → cache result → return
 *   Request 2:  Cache hit → 0.1ms → return (no DB query!)
 *   Requests 3-1000: Same — cache hit → 0.1ms each
 *
 * Real numbers from Netflix (2019):
 *   Without caching: 8+ million DB queries per second
 *   With caching: ~100,000 DB queries per second (98.75% reduction)
 *
 * ============================================================
 * CACHE INVALIDATION — The Hard Part
 * ============================================================
 *
 * "There are only two hard things in Computer Science: cache invalidation and naming things."
 * — Phil Karlton (famous quote, referenced in countless interviews)
 *
 * Problem: If you cache "Project A has 5 tasks" and then someone adds a task,
 * the cache still says 5. The cache is STALE (out of date).
 *
 * Solutions:
 *
 * 1. TTL (Time-To-Live) — expire after N minutes
 *    Pro: Simple. Con: Stale data for up to N minutes.
 *    Our config: projects = 10 min TTL, tasks = 5 min TTL.
 *
 * 2. @CacheEvict — delete the cache entry when data changes
 *    Pro: Immediately fresh. Con: Next read hits DB again.
 *    Used on: updateProject(), deleteProject(), updateTask()...
 *
 * 3. @CachePut — update the cache entry when data changes
 *    Pro: Always fresh. Con: Writes happen twice (DB + cache).
 *    Used on: update methods where the return value should stay cached.
 *
 * ============================================================
 * WHY JSON SERIALIZATION (not Java binary)?
 * ============================================================
 *
 * By default, Spring would use Java binary serialization (ObjectOutputStream).
 * Problems with binary serialization:
 *   1. NOT human-readable in Redis (can't inspect in redis-cli)
 *   2. Breaks when class structure changes (serialVersionUID issues)
 *   3. Slow compared to JSON
 *   4. Java-only — can't share cached data with Python/Node.js services
 *
 * JSON serialization:
 *   1. Human-readable: you can see the data in redis-cli
 *   2. Flexible: survives adding/removing fields (Jackson ignores unknown fields)
 *   3. Interoperable: any language can read it
 *
 * Redis key format: "taskflow:projects::1" (prefix + cache name + "::" + key)
 *
 * ============================================================
 * @EnableCaching — IMPORTANT
 * ============================================================
 *
 * Without @EnableCaching, all @Cacheable/@CacheEvict annotations are IGNORED.
 * Same pattern as @EnableScheduling for @Scheduled.
 * Spring needs explicit opt-in for cross-cutting features.
 *
 * INTERVIEW Q: What is TTL in caching?
 * A: Time-To-Live. After this duration, the cache entry expires automatically.
 *    Prevents stale data from being served indefinitely.
 *    Trade-off: shorter TTL = fresher data but more DB hits.
 *               longer TTL = stale data possible but fewer DB hits.
 *
 * INTERVIEW Q: What is the difference between @Cacheable and @CachePut?
 * A: @Cacheable — check cache first, run method ONLY on miss, cache result.
 *    @CachePut — ALWAYS run method, always update cache with result.
 *    Use @Cacheable for reads (avoid method if cached).
 *    Use @CachePut for writes (method must run, but update cache too).
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * RedisTemplate — the low-level tool for direct Redis operations.
     *
     * Used when you need raw Redis commands in your code:
     *   redisTemplate.opsForValue().set("key", value)
     *   redisTemplate.opsForValue().get("key")
     *   redisTemplate.delete("key")
     *   redisTemplate.expire("key", Duration.ofMinutes(5))
     *
     * For @Cacheable/@CacheEvict, Spring uses CacheManager (below), not this directly.
     * But you might use RedisTemplate in services that need custom caching logic.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // Keys as plain Strings: "taskflow:projects::1" (human-readable in redis-cli)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Values as JSON: {"id":1,"name":"My Project",...} (not binary gibberish)
        GenericJackson2JsonRedisSerializer jsonSerializer = buildJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * CacheManager — what Spring uses under the hood for @Cacheable/@CacheEvict.
     *
     * Different caches have different TTLs:
     *   "projects" → 10 minutes (projects change infrequently)
     *   "tasks"    → 5 minutes  (tasks change more often — status updates)
     *   "users"    → 30 minutes (user profiles rarely change)
     *
     * All keys are prefixed with "taskflow:" to namespace our app's data
     * and avoid collisions if Redis is shared with other apps.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = buildCacheConfig(Duration.ofMinutes(10));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        "projects", buildCacheConfig(Duration.ofMinutes(10)),
                        "tasks",    buildCacheConfig(Duration.ofMinutes(5)),
                        "users",    buildCacheConfig(Duration.ofMinutes(30))
                ))
                .build();
    }

    // ---- Private Helpers ----

    private RedisCacheConfiguration buildCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeKeysWith(pair(new StringRedisSerializer()))
                .serializeValuesWith(pair(buildJsonSerializer()))
                .disableCachingNullValues()     // Don't cache null — next miss will re-check DB
                .prefixCacheNameWith("taskflow:");
    }

    /**
     * Builds a Jackson-based JSON serializer configured for:
     * 1. JavaTimeModule — handles LocalDate, LocalDateTime, etc.
     * 2. ISO date strings — "2026-06-11" not [2026,6,11] (array format)
     * 3. Type info — stores "@class" in JSON so deserialization knows the exact type
     *
     * The type info is REQUIRED for GenericJackson2JsonRedisSerializer to reconstruct
     * the correct Java object (e.g., ProjectResponse, not just a LinkedHashMap).
     */
    private GenericJackson2JsonRedisSerializer buildJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();

        // Handle Java 8+ date/time types
        mapper.registerModule(new JavaTimeModule());

        // Write dates as "2026-06-11" strings, not [2026, 6, 11] number arrays
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Include class type in JSON so Jackson can deserialize back to correct type
        // Without this: {"id":1,...} — Jackson doesn't know the class
        // With this:    {"@class":"com.taskflow.dto.response.ProjectResponse","id":1,...}
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    private <T> RedisSerializationContext.SerializationPair<T> pair(RedisSerializer<T> serializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer(serializer);
    }
}
