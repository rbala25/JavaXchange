package com.rishibala.bots;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
        private final int capacity;
        private final double refillRate;
        private int tokens;
        private Instant lastRefillTime;

        public RateLimiter(int capacity, double refillRate) {
            this.capacity = capacity;
            this.refillRate = refillRate;
            this.tokens = capacity;
            this.lastRefillTime = Instant.now();
        }

        public synchronized boolean allowRequest() {
            refillTokens();
            if (tokens > 0) {
                tokens--;
                return true;
            }
            return false;
        }

        private void refillTokens() {
            Instant now = Instant.now();
            long timeElapsed = TimeUnit.NANOSECONDS.toSeconds(now.getNano() - lastRefillTime.getNano());

            int tokensToAdd = (int) (timeElapsed * refillRate);

            tokens = Math.min(tokens + tokensToAdd, capacity);

            lastRefillTime = now;
        }
}
