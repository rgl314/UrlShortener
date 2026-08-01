package com.ragul.UrlShortener.dto;

import com.ragul.UrlShortener.model.ClickEvent;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UrlAnalyticsResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private int totalClicks;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private List<ClickEventResponse> recentClicks;
    private Map<String, Integer> clicksByReferrer;
    private Map<String, Integer> clicksByHour;
    private Map<String, Integer> clicksByDay;

}
