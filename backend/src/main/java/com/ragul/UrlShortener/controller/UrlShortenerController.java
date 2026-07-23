package com.ragul.UrlShortener.controller;

import com.ragul.UrlShortener.dto.ShortenUrlRequest;
import com.ragul.UrlShortener.dto.ShortenUrlResponse;
import com.ragul.UrlShortener.service.RateLimitService;
import com.ragul.UrlShortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class UrlShortenerController {

    private final UrlShortenerService urlShortenerService;
    private final RateLimitService rateLimitService;

    @PostMapping("/shorten")
    public ResponseEntity<Object> shortenUrl(
            @Valid @RequestBody ShortenUrlRequest shortenUrlRequest,
            HttpServletRequest httpRequest
    ){
        String clientIp = getClientIP(httpRequest);
        if(!rateLimitService.isAllowed(clientIp)){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    Map.of("error", "Rate limit exceeded",
                            "remainingRequests", rateLimitService.getRemainingRequests(clientIp),
                            "timeUntilReset", rateLimitService.getTimeUntilReset(clientIp)
                    )
            );
        }
        try{
            ShortenUrlResponse response = urlShortenerService.shortenUrl(shortenUrlRequest, clientIp);
            return ResponseEntity.ok(response);
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    private String getClientIP(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if(xForwardedFor != null && !xForwardedFor.isBlank()){
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = httpRequest.getHeader("X-Real-IP");
        if(xRealIp != null && !xRealIp.isBlank()){
            return xRealIp;
        }

        return httpRequest.getRemoteAddr();
    }

}
