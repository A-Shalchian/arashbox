package com.arashbox.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_MS = 60_000; // 1 minute

    private final ConcurrentHashMap<String, long[]> requestLog = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/api/execute".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = getClientIp(request);
        long now = System.currentTimeMillis();

        long[] timestamps = requestLog.compute(ip, (key, existing) -> {
            if (existing == null) {
                long[] ts = new long[MAX_REQUESTS];
                ts[0] = now;
                return ts;
            }
            // Shift out expired entries, count active ones
            long cutoff = now - WINDOW_MS;
            long[] active = new long[MAX_REQUESTS];
            int count = 0;
            for (long t : existing) {
                if (t > cutoff && count < MAX_REQUESTS) {
                    active[count++] = t;
                }
            }
            if (count < MAX_REQUESTS) {
                active[count] = now;
                return active;
            }
            return existing; // full — don't add
        });

        // Count how many timestamps are in the current window
        long cutoff = now - WINDOW_MS;
        int count = 0;
        for (long t : timestamps) {
            if (t > cutoff) count++;
        }

        if (count > MAX_REQUESTS) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Max 10 requests per minute.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
