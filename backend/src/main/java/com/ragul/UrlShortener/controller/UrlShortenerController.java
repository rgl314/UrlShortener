package com.ragul.UrlShortener.controller;

import com.ragul.UrlShortener.dto.ShortenUrlRequest;
import com.ragul.UrlShortener.dto.ShortenUrlResponse;
import com.ragul.UrlShortener.dto.UrlAnalyticsResponse;
import com.ragul.UrlShortener.dto.UrlStatsResponse;
import com.ragul.UrlShortener.service.RateLimitService;
import com.ragul.UrlShortener.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
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
        String clientIp = getClientIp(httpRequest);
        if(!rateLimitService.isAllowed(clientIp)){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                    Map.of("error", "Rate limit exceeded",
                            "remainingRequests", rateLimitService.getRemainingRequests(clientIp),
                            "timeUntilReset", rateLimitService.getTimeUntilReset(clientIp)
                    )
            );
        }
        ShortenUrlResponse response = urlShortenerService.shortenUrl(shortenUrlRequest, clientIp);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToUrl(
            @PathVariable String shortCode,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        Optional<String> originalUrl = urlShortenerService.getOriginalUrl(shortCode);

        if (originalUrl.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        urlShortenerService.recordClick(shortCode, clientIp, userAgent, referer);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl.get()))
                .build();
    }

    @GetMapping("/stats/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getUrlStats(@PathVariable String shortCode){
        UrlStatsResponse urlStatsResponse = urlShortenerService.getUrlStats(shortCode);
        return new ResponseEntity<>(urlStatsResponse, HttpStatus.OK);
    }

    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<?> getUrlAnalytics(@PathVariable String shortCode){
        UrlAnalyticsResponse urlAnalytics = urlShortenerService.getUrlAnalytics(shortCode);
        return new ResponseEntity<>(urlAnalytics, HttpStatus.OK);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<?> deleteUrl(@PathVariable String shortCode){
        boolean deleted = urlShortenerService.deleteUrl(shortCode);

        if(deleted){
            return ResponseEntity.ok(Map.of("message","URL deleted successfully"));
        }else{
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error","Short code not found"));
        }
    }

    private String getClientIp(HttpServletRequest httpRequest) {
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
