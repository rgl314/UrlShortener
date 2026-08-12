package com.ragul.UrlShortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record ShortenUrlRequest(
        @NotBlank(message = "Original URl is required")
        @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
        String originalUrl,

        @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
        String customAlias,

        @Future
        LocalDateTime expiresAt
) {
}
