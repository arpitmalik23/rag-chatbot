package com.ragbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragbot.model.ChatTurn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wraps Upstash Redis for two things:
 *  - doc:{sessionId}      -> hash of the currently active document's metadata
 *  - history:{sessionId}  -> list of cached ChatTurn JSON blobs (most recent last)
 */
@Service
public class RedisService {

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${redis.history.max-turns:50}")
    private int maxHistoryTurns;

    @Value("${redis.ttl.seconds:86400}")
    private int ttlSeconds;

    public RedisService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    public void saveDocMetadata(String sessionId, String docId, String filename, int chunkCount) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "doc:" + sessionId;
            Map<String, String> fields = new HashMap<>();
            fields.put("docId", docId);
            fields.put("filename", filename);
            fields.put("chunkCount", String.valueOf(chunkCount));
            jedis.hset(key, fields);
            jedis.expire(key, ttlSeconds);
        }
    }
   public boolean hasAnyDocument(String sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("doc:" + sessionId);
        }
    }

    public Map<String, String> getDocMetadata(String sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll("doc:" + sessionId);
        }
    }

    public void appendChatTurn(String sessionId, String question, String answer) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "history:" + sessionId;
            ChatTurn turn = new ChatTurn(question, answer, System.currentTimeMillis());
            try {
                String json = objectMapper.writeValueAsString(turn);
                jedis.rpush(key, json);
                jedis.ltrim(key, -maxHistoryTurns, -1);
                jedis.expire(key, ttlSeconds);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to cache chat turn in Redis", e);
            }
        }
    }

    public List<ChatTurn> getHistory(String sessionId) {
        List<ChatTurn> turns = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            List<String> raw = jedis.lrange("history:" + sessionId, 0, -1);
            for (String json : raw) {
                try {
                    turns.add(objectMapper.readValue(json, ChatTurn.class));
                } catch (Exception ignored) {
                    // skip malformed entries rather than failing the whole read
                }
            }
        }
        return turns;
    }
}
