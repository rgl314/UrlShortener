package com.ragul.UrlShortener.dto;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
public class ShortenUrlResponse {

    private String shortUrl;
    private String shortCode;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

}
