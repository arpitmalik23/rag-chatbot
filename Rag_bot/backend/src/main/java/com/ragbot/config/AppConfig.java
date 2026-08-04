package com.ragbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.net.URI;

@Configuration
public class AppConfig {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}")
    private String geminiBaseUrl;

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api-key:}")
    private String qdrantApiKey;

    @Value("${upstash.redis.url}")
    private String redisUrl;

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    // ---------- CORS ----------

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }

    // ---------- Gemini WebClient ----------

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(geminiBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // ---------- Groq WebClient ----------

    @Bean
    public WebClient groqWebClient() {
        return WebClient.builder()
                .baseUrl(groqBaseUrl)
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Authorization", "Bearer " + groqApiKey)
                .build();
    }

    // ---------- Qdrant WebClient ----------

    @Bean
    public WebClient qdrantWebClient() {
        WebClient.Builder builder = WebClient.builder()
                .baseUrl(qdrantUrl)
                .defaultHeader("Content-Type", "application/json");
        if (qdrantApiKey != null && !qdrantApiKey.isBlank()) {
            builder.defaultHeader("api-key", qdrantApiKey);
        }
        return builder.build();
    }

    // ---------- Upstash Redis (Jedis over TLS) ----------

    @Bean
    public JedisPool jedisPool() {
        URI uri = URI.create(redisUrl); // e.g. rediss://default:<password>@<host>:<port>
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        return new JedisPool(poolConfig, uri);
    }
}
