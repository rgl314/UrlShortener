package com.ragul.UrlShortener.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class ClickEventResponse {

    private LocalDateTime clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String city;

}
