package com.ragbot.config;

import com.ragbot.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class JwtAuthFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> jwtFilterRegistration() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                    throws ServletException, IOException {
                        
                   if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                        chain.doFilter(req, res);
                        return;
                    }        

                String header = req.getHeader("Authorization");
                if (header == null || !header.startsWith("Bearer ")) {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Missing or invalid Authorization header.\"}");
                    return;
                }

                try {
                    String username = jwtUtil.validateAndGetUsername(header.substring(7));
                    req.setAttribute("username", username);
                    chain.doFilter(req, res);
                } catch (Exception e) {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"Invalid or expired token.\"}");
                }
            }
        };

        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/api/upload", "/api/chat", "/api/chat/*");
        return registration;
    }
}