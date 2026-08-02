package com.ragul.UrlShortener.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "url-shortener")
@Validated
@Getter
@Setter
public class UrlShortenerConfig {

    @NotBlank(message = "Base URL cannot be blank")
    private String baseUrl;

    @Valid
    private ShortCode shortCode = new ShortCode();

    @Getter
    @Setter
    public static class ShortCode{

        @Min(value = 4, message = "Short code length must be at least 4")
        private int length;

        @Min(value = 1, message = "Maximum attempts must be at least 1")
        private int maxAttempts;
    }

    @Valid
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class RateLimit{

        @Min(value = 1, message = "Maximum requests per minute must be at least 1")
        private int maxRequestsPerMinute;

        @Min(value = 1, message = "Maximum requests per hour must be at least 1")
        private int maxRequestsPerHour;

        @NotNull(message = "Minute window is required")
        private Duration maxMinuteWindow;

        @NotNull(message = "Hour window is required")
        private Duration maxHourWindow;
    }

    @Valid
    private Cache cache = new Cache();

    @Getter
    @Setter
    public static class Cache{

        @Min(value = 1, message = "Cache TTL must be at least 1 minute")
        private int ttlMinutes;
    }

    @Valid
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Cleanup{

        @Min(value = 1, message = "Cleanup interval must be at least 1 minute")
        private int intervalMinutes;

        @Min(value = 1, message = "Expired URL batch size must be at least 1")
        private int expiredUrlsBatchSize;
    }

}
