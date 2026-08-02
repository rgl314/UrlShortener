package com.ragul.UrlShortener.service;

import com.ragul.UrlShortener.config.UrlShortenerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final UrlShortenerConfig urlShortenerConfig;

    private static final String MINUTE_KEY_PREFIX = "ratelimit:minute:";
    private static final String HOUR_KEY_PREFIX = "ratelimit:hour:";

    // to check whether a client(IP address) is allowed to make a request at a current moment
    public boolean isAllowed(String clientIp) {

        String minuteKey = MINUTE_KEY_PREFIX + clientIp;
        String hourKey = HOUR_KEY_PREFIX + "hour:" + clientIp;

        Long minuteCount = redisTemplate.opsForValue().increment(minuteKey);
        if(minuteCount != null && minuteCount == 1L){
            redisTemplate.expire(minuteKey, urlShortenerConfig.getRateLimit().getMaxMinuteWindow());
        }

        Long hourCount = redisTemplate.opsForValue().increment(hourKey);
        if(hourCount != null && hourCount == 1L){
            redisTemplate.expire(hourKey, urlShortenerConfig.getRateLimit().getMaxHourWindow());
        }

        if(minuteCount != null && minuteCount > urlShortenerConfig.getRateLimit().getMaxRequestsPerMinute()){
            log.warn("Minute limit exceeded for {}", clientIp);
            return false;
        }

        if(hourCount != null && hourCount > urlShortenerConfig.getRateLimit().getMaxRequestsPerHour()){
            log.warn("Hour limit exceeded for {}", clientIp);
            return false;
        }

        return true;
    }

    public int getRemainingRequests(String clientIp) {
        String minuteKey = MINUTE_KEY_PREFIX + clientIp;
        String minuteCount = redisTemplate.opsForValue().get(minuteKey);

        if(minuteCount == null){
            return urlShortenerConfig.getRateLimit().getMaxRequestsPerMinute();
        }

        int used = Integer.parseInt(minuteCount);

        return Math.max(0, urlShortenerConfig.getRateLimit().getMaxRequestsPerMinute() - used);
    }

    public long getTimeUntilReset(String clientIp) {
        String minuteKey = MINUTE_KEY_PREFIX + clientIp;

        Long ttl = redisTemplate.getExpire(minuteKey);

        if(ttl == null || ttl < 0){
            return 0;
        }
        return ttl;
    }

}
