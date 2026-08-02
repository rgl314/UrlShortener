package com.ragul.UrlShortener.service;

import com.ragul.UrlShortener.config.UrlShortenerConfig;
import com.ragul.UrlShortener.dto.*;
import com.ragul.UrlShortener.exception.DuplicateAliasException;
import com.ragul.UrlShortener.exception.RateLimitExceededException;
import com.ragul.UrlShortener.exception.UrlDataNotFound;
import com.ragul.UrlShortener.model.ClickEvent;
import com.ragul.UrlShortener.model.UrlData;
import com.ragul.UrlShortener.repository.ClickEventRepository;
import com.ragul.UrlShortener.repository.UrlDataRepository;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UrlDataRepository urlDataRepository;
    private final ClickEventRepository clickEventRepository;
    private final UrlShortenerConfig urlShortenerConfig;

    private static final String BASE_62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHILJKLMNOPQRSTUVWXYZ";

    public ShortenUrlResponse shortenUrl(ShortenUrlRequest urlRequest, String clientIp) {
        String shortCode = urlRequest.customAlias();

        if(shortCode == null || shortCode.trim().isEmpty()){
            shortCode = generateUniqueShortCode();
        }
        else{
            shortCode = shortCode.trim();
            if(urlDataRepository.existsByShortCode(shortCode)){
                throw new DuplicateAliasException("custom alias already exists: " + shortCode);
            }
        }

        UrlData urlData = UrlData.builder()
                .originalUrl(urlRequest.originalUrl())
                .shortCode(shortCode)
                .createdAt(LocalDateTime.now())
                .expiresAt(urlRequest.expiresAt())
                .createdBy(clientIp)
                .clickCount(0)
                .isActive(true)
                .clickEvents(new ArrayList<>())
                .build();

        urlDataRepository.save(urlData);
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
        String normalizedBaseUrl = urlShortenerConfig.getBaseUrl().endsWith("/") ?
                urlShortenerConfig.getBaseUrl().substring(0, urlShortenerConfig.getBaseUrl().length() - 1) :
                urlShortenerConfig.getBaseUrl();

        return normalizedBaseUrl + "/api/" + shortCode;
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        try{
            redisTemplate.opsForValue().set("url:" + shortCode, originalUrl, urlShortenerConfig.getCache().getTtlMinutes(), TimeUnit.MINUTES);
        }
        catch (Exception e){
            log.warn("Failed to cache URL for  {}:{}", shortCode, e.getMessage());
        }
    }

    private String generateUniqueShortCode() {
        for(int attempt = 0; attempt < urlShortenerConfig.getShortCode().getMaxAttempts(); attempt++){
            String code = generateRandomBase62();
            if(!urlDataRepository.existsByShortCode(code)){
                return code;
            }
        }
        throw new RateLimitExceededException("Failed to generate unique short code after " + urlShortenerConfig.getShortCode().getMaxAttempts());
    }

    private String generateRandomBase62() {
        StringBuilder sb = new StringBuilder(urlShortenerConfig.getShortCode().getLength());
        for(int i=0;i<urlShortenerConfig.getShortCode().getLength();i++){
            int index = ThreadLocalRandom.current().nextInt(BASE_62_CHARS.length());
            sb.append(BASE_62_CHARS.charAt(index));
        }
        return sb.toString();
    }

    public Optional<String> getOriginalUrl(String shortCode) {
        String cacheUrl = getCachedUrl(shortCode);
        if (cacheUrl != null) {
            return Optional.of(cacheUrl);
        }

        UrlData urlData = urlDataRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlDataNotFound("Url Data does not exists!"));

        if(urlData != null  && urlData.isActive()){
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
        UrlData urlData = urlDataRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlDataNotFound("Url Data does not exists!"));

        if(urlData != null && urlData.isActive()){
            urlData.setClickCount(urlData.getClickCount() + 1);

            ClickEvent clickEvent = ClickEvent.builder()
                    .urlData(urlData)
                    .clickedAt(LocalDateTime.now())
                    .ipAddress(clientIp)
                    .userAgent(userAgent)
                    .referer(referer)
                    .build();

            clickEventRepository.save(clickEvent);
            urlDataRepository.save(urlData);

            log.debug("Recorder click for short code: {}", shortCode);
        }
    }

    public UrlStatsResponse getUrlStats(String shortCode) {
        UrlData urlData = urlDataRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlDataNotFound("Url Data does not exists!"));

        return UrlStatsResponse.builder()
                        .shortCode(urlData.getShortCode())
                        .originalUrl(urlData.getOriginalUrl())
                        .clickCount(urlData.getClickCount())
                        .createdAt(urlData.getCreatedAt())
                        .expiresAt(urlData.getExpiresAt())
                        .isActive(urlData.isActive())
                        .createdBy(urlData.getCreatedBy())
                        .build();
    }

    public UrlAnalyticsResponse getUrlAnalytics(String shortCode) {
        UrlData urlData = urlDataRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlDataNotFound("Url Data does not exists!"));

        List<ClickEvent> clicks = clickEventRepository.findAllByUrlData(urlData);

        Map<String, Integer> clicksByReferrer = clicks.stream()
                .filter(c->c.getReferer() != null)
                .collect(Collectors.groupingBy(
                        ClickEvent::getReferer, Collectors.summingInt(e->1)
                ));

        Map<String, Integer> clicksByHour = clicks.stream()
                .collect(Collectors.groupingBy(
                        c->c.getClickedAt().getHour() + ":00",
                        Collectors.summingInt(e->1)
                ));

        Map<String, Integer> clicksByDay = clicks.stream()
                .collect(Collectors.groupingBy(
                        c->c.getClickedAt().toLocalDate().toString(),
                        Collectors.summingInt(e->1)
                ));

        List<ClickEvent> recentClicks = clicks.stream()
                .sorted((a,b)->b.getClickedAt().compareTo(a.getClickedAt()))
                .limit(10)
                .toList();

        List<ClickEventResponse> clickEventResponses = recentClicks.stream().map(r ->
                ClickEventResponse.builder()
                        .clickedAt(r.getClickedAt())
                        .ipAddress(r.getIpAddress())
                        .userAgent(r.getUserAgent())
                        .referer(r.getReferer())
                        .city(r.getCity())
                        .country(r.getCountry())
                        .build()
                ).toList();

        return UrlAnalyticsResponse.builder()
                        .shortCode(shortCode)
                        .shortUrl(buildShortUrl(shortCode))
                        .originalUrl(urlData.getOriginalUrl())
                        .totalClicks(urlData.getClickCount())
                        .createdAt(urlData.getCreatedAt())
                        .expiresAt(urlData.getExpiresAt())
                        .recentClicks(clickEventResponses)
                        .clicksByReferrer(clicksByReferrer)
                        .clicksByHour(clicksByHour)
                        .clicksByDay(clicksByDay)
                        .build();

    }

    public boolean deleteUrl(String shortCode) {
        UrlData urlData = urlDataRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlDataNotFound("Url Data does not exists!"));
        if(urlData!=null){
            urlData.setActive(false);
            urlDataRepository.save(urlData);
            deleteCacheUrl(shortCode);
            log.info("Deleted URL: {}",shortCode);
            return true;
        }
        return false;
    }

    private void deleteCacheUrl(String shortCode) {
        try{
            redisTemplate.delete("url:"+shortCode);
        }catch (Exception e){
            log.warn("Failed to delete cached URL for {}: {}",shortCode, e.getMessage());
        }
    }

}
