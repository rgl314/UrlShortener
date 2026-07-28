package com.ragul.UrlShortener.service;

import com.ragul.UrlShortener.dto.ShortenUrlRequest;
import com.ragul.UrlShortener.dto.ShortenUrlResponse;
import com.ragul.UrlShortener.dto.UrlAnalyticsResponse;
import com.ragul.UrlShortener.dto.UrlStatsResponse;
import com.ragul.UrlShortener.model.ClickEvent;
import com.ragul.UrlShortener.model.UrlData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, UrlData> urlMappings = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<ClickEvent>> clickAnalytics = new ConcurrentHashMap<>();

    @Value("${url-shortener.base-url}")
    private String baseUrl;

    @Value("${url-shortener.short-code.length}")
    private int shortCodeLength;

    @Value("${url-shortener.short-code.max-attempts}")
    private int maxGenerationAttempts;

    @Value("${url-shortener.cache.ttl-minutes}")
    private int cacheTtlMinutes;

    private static final String BASE_62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHILJKLMNOPQRSTUVWXYZ";

    public ShortenUrlResponse shortenUrl(ShortenUrlRequest urlRequest, String clientIp) {
        String shortCode = urlRequest.customAlias();

        if(shortCode == null || shortCode.trim().isEmpty()){
            shortCode = generateUniqueShortCode();
        }
        else{
            shortCode = shortCode.trim();
            if(shortCodeExists(shortCode)){
                throw new IllegalArgumentException("custom alias already exists: " + shortCode);
            }
        }

        UrlData urlData = UrlData.builder()
                .originalUrl(urlRequest.originalUrl())
                .shortCode(shortCode)
                .expiresAt(urlRequest.expiresAt())
                .createdAt(LocalDateTime.now())
                .createdBy(clientIp)
                .clickCount(0)
                .isActive(true)
                .clickEvents(new ArrayList<>())
                .build();

        urlMappings.put(shortCode, urlData);
        clickAnalytics.put(shortCode, new ArrayList<>());

        cacheUrl(shortCode, urlRequest.originalUrl());

        log.info("Created short URL: {} -> {}", shortCode, urlRequest.originalUrl());

        return ShortenUrlResponse.builder()
                .shortUrl(buildShortUrl(shortCode))
                .shortCode(shortCode)
                .originalUrl(urlRequest.originalUrl())
                .createdAt(urlData.getCreatedAt())
                .expiresAt(urlData.getExpiresAt())
                .build();
    }

    private String buildShortUrl(String shortCode) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/api/" + shortCode;
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        try{
            redisTemplate.opsForValue().set("url:" + shortCode, originalUrl, cacheTtlMinutes, TimeUnit.MINUTES);
        }
        catch (Exception e){
            log.warn("Failed to cache URL for  {}:{}", shortCode, e.getMessage());
        }
    }

    private String generateUniqueShortCode() {
        for(int attempt = 0; attempt < maxGenerationAttempts; attempt++){
            String code = generateRandomBase62();
            if(!shortCodeExists(code)){
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique short code after " + maxGenerationAttempts);
    }

    private String generateRandomBase62() {
        StringBuilder sb = new StringBuilder(shortCodeLength);
        for(int i=0;i<shortCodeLength;i++){
            int index = ThreadLocalRandom.current().nextInt(BASE_62_CHARS.length());
            sb.append(BASE_62_CHARS.charAt(index));
        }
        return sb.toString();
    }

    private boolean shortCodeExists(String code) {
        return urlMappings.containsKey(code);
    }

    public Optional<String> getOriginalUrl(String shortCode) {
        String cacheUrl = getCachedUrl(shortCode);
        if (cacheUrl != null) {
            return Optional.of(cacheUrl);
        }

        UrlData urlData = urlMappings.get(shortCode);
        if(urlData != null && urlData.isActive()){
            if(isExpired(urlData)){
                urlData.setActive(false);
                return Optional.empty();
            }
            cacheUrl(shortCode, urlData.getOriginalUrl());
            return Optional.of(urlData.getOriginalUrl());
        }
        return Optional.empty();
    }

    private boolean isExpired(UrlData urlData) {
        return urlData.getExpiresAt() != null && urlData.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private String getCachedUrl(String shortCode) {
        try {
            return (String) redisTemplate.opsForValue().get("url:" + shortCode);
        } catch (Exception e) {
            log.warn("Failed to reach cached URL for {}:{}", shortCode, e.getMessage());
            return null;
        }
    }

    public void recordClick(String shortCode, String clientIp, String userAgent, String referer) {
        UrlData urlData = urlMappings.get(shortCode);
        if(urlData != null && urlData.isActive()){
            urlData.setClickCount(urlData.getClickCount() + 1);

            ClickEvent clickEvent = ClickEvent.builder()
                    .timestamp(LocalDateTime.now())
                    .ipAddress(clientIp)
                    .userAgent(userAgent)
                    .referer(referer)
                    .build();

            clickAnalytics.get(shortCode).add(clickEvent);
            log.debug("Recorder click for short code: {}", shortCode);
        }
    }

    public Optional<UrlStatsResponse> getUrlStats(String shortCode) {
        UrlData urlData = urlMappings.get(shortCode);
        if(urlData == null){
            return Optional.empty();
        }
        return Optional.of(UrlStatsResponse.builder()
                        .shortCode(urlData.getShortCode())
                        .originalUrl(urlData.getOriginalUrl())
                        .clickCount(urlData.getClickCount())
                        .createdAt(urlData.getCreatedAt())
                        .expiresAt(urlData.getExpiresAt())
                        .isActive(urlData.isActive())
                        .createdBy(urlData.getCreatedBy())
                        .build());
    }

    public Optional<UrlAnalyticsResponse> getUrlAnalytics(String shortCode) {
        UrlData urlData = urlMappings.get(shortCode);
        if(urlData == null){
            return Optional.empty();
        }

        List<ClickEvent> clicks = clickAnalytics.getOrDefault(urlData.getShortCode(), new ArrayList<>());

        Map<String, Integer> clicksByReferrer = clicks.stream()
                .filter(c->c.getReferer() != null)
                .collect(Collectors.groupingBy(
                        ClickEvent::getReferer, Collectors.summingInt(e->1)
                ));

        Map<String, Integer> clicksByHour = clicks.stream()
                .collect(Collectors.groupingBy(
                        c->c.getTimestamp().getHour() + ":00",
                        Collectors.summingInt(e->1)
                ));

        Map<String, Integer> clicksByDay = clicks.stream()
                .collect(Collectors.groupingBy(
                        c->c.getTimestamp().toLocalDate().toString(),
                        Collectors.summingInt(e->1)
                ));

        List<ClickEvent> recentClicks = clicks.stream()
                .sorted((a,b)->b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .toList();

        return Optional.of(
                UrlAnalyticsResponse.builder()
                        .shortCode(shortCode)
                        .originalUrl(urlData.getOriginalUrl())
                        .totalClicks(urlData.getClickCount())
                        .createdAt(urlData.getCreatedAt())
                        .expiresAt(urlData.getExpiresAt())
                        .recentClicks(recentClicks)
                        .clicksByReferrer(clicksByReferrer)
                        .clicksByHour(clicksByHour)
                        .clicksByDay(clicksByDay)
                        .build()
        );

    }
}
