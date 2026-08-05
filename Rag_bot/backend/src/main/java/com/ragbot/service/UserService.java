package com.ragbot.service;

import com.ragbot.util.PasswordUtil;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

@Service
public class UserService {

    private final JedisPool jedisPool;

    public UserService(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    /** Returns true if the user was newly created; false if the username was already taken. */
    public boolean register(String username, String password) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "user:" + username.toLowerCase();
            String hashed = PasswordUtil.hash(password);
            // NX = only set if it doesn't already exist -> avoids a race between check-then-set
            String result = jedis.set(key, hashed, SetParams.setParams().nx());
            return "OK".equals(result);
        }
    }

    public boolean verifyLogin(String username, String password) {
        try (Jedis jedis = jedisPool.getResource()) {
            String stored = jedis.get("user:" + username.toLowerCase());
            if (stored == null) return false;
            return PasswordUtil.verify(password, stored);
        }
    }
}