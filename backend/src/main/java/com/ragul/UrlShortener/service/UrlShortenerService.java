package com.ragul.UrlShortener.service;

import com.ragul.UrlShortener.dto.ShortenUrlRequest;
import com.ragul.UrlShortener.dto.ShortenUrlResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortenerService {

    private final RedisTemplate<String, Object> redisTemplate;

    public ShortenUrlResponse shortenUrl(ShortenUrlRequest urlRequest, String clientIp) {

    }

}
