package com.ragul.UrlShortener.service;

import com.ragul.UrlShortener.model.RateLimitData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${url-shortener.rate-limit.requests-per-minute}")
    private int requestPerMinute;

    @Value("${url-shortener.rate-limit.requests-per-hour}")
    private int requestPerHour;

    private static final String REDIS_KEY_PREFIX = "ratelimit:";

    private final ConcurrentHashMap<String, RateLimitData> map = new ConcurrentHashMap<>();

    // to check whether a client(IP address) is allowed to make a request at a current moment
    public boolean isAllowed(String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + clientIp;

        LocalDateTime currentTime = LocalDateTime.now();

        RateLimitData rateLimitData = getRateLimitDataFromRedis(redisKey);

        // new IP (no data yet)
        if(rateLimitData == null){
            rateLimitData = map.computeIfAbsent(redisKey, k -> RateLimitData.builder()
                    .hourCount(0)
                    .minuteCount(0)
                    .hourWindowStart(currentTime)
                    .minuteWindowStart(currentTime)
                    .build());
        }

        // for 1 min client can give only 2 requests
        if(isWithinMinuteWindow(rateLimitData, currentTime)){
            if(rateLimitData.getMinuteCount() >= requestPerMinute){
                log.warn("Minute limit exceeded for {}", clientIp);
                return false;
            }
        }
        else{
            rateLimitData.setMinuteCount(0);
            rateLimitData.setMinuteWindowStart(currentTime);
        }

        // for 1 hour client can give only 10 requests
        if(isWithinHourWindow(rateLimitData, currentTime)){
            if(rateLimitData.getHourCount() >= requestPerHour){
                log.warn("Hour limit exceeded for {}", clientIp);
                return false;
            }
        }
        else{
            rateLimitData.setHourCount(0);
            rateLimitData.setHourWindowStart(currentTime);
        }

        rateLimitData.setMinuteCount(rateLimitData.getMinuteCount() + 1);
        rateLimitData.setHourCount(rateLimitData.getHourCount() + 1);

        saveRateLimitDataToRedis(redisKey, rateLimitData);

        return true;
    }

    private boolean isWithinHourWindow(RateLimitData rateLimitData, LocalDateTime currentTime) {
        return rateLimitData.getHourWindowStart() != null &&
                ChronoUnit.HOURS.between(rateLimitData.getHourWindowStart(), currentTime) < 1;
    }

    private boolean isWithinMinuteWindow(RateLimitData rateLimitData, LocalDateTime currentTime) {
        return rateLimitData.getMinuteWindowStart() != null &&
                ChronoUnit.MINUTES.between(rateLimitData.getMinuteWindowStart(), currentTime) < 1;
    }

    private void saveRateLimitDataToRedis(String redisKey, RateLimitData rateLimitData) {
        try{
            redisTemplate.opsForValue().set(redisKey, rateLimitData, 1, TimeUnit.HOURS);
        }
        catch (Exception e){
            log.warn("Failed to save Rate-limit data to Redis: {}", e.getMessage());
        }
    }

    private RateLimitData getRateLimitDataFromRedis(String redisKey) {
        try{
            return (RateLimitData) redisTemplate.opsForValue().get(redisKey);
        }
        catch (Exception e){
            log.warn("failed to get Rate-limit data from the redis: {}", e.getMessage());
            throw new IllegalStateException("Redis is unavailable", e);
        }
    }

    public int getRemainingRequests(String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + clientIp;
        RateLimitData data = getRateLimitDataFromRedis(redisKey);

        if(data == null){
            return requestPerMinute;
        }

        LocalDateTime now = LocalDateTime.now();
        if(!isWithinMinuteWindow(data, now)){
            return requestPerMinute;
        }

        return Math.max(0, requestPerMinute - data.getMinuteCount());
    }

    public long getTimeUntilReset(String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + clientIp;
        RateLimitData data = getRateLimitDataFromRedis(redisKey);

        if (data == null) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        if (data.getMinuteCount() >= requestPerMinute) {
            LocalDateTime nextMinute = data.getMinuteWindowStart()
                    .plusMinutes(1);
            return ChronoUnit.SECONDS.between(now, nextMinute);
        }

        if (data.getHourCount() >= requestPerHour) {
            LocalDateTime nextHour = data.getHourWindowStart()
                    .plusHours(1);
            return ChronoUnit.SECONDS.between(now, nextHour);
        }

        return 0;
    }
}
