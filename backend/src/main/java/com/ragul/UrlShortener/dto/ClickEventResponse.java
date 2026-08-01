package com.ragul.UrlShortener.dto;

import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEventResponse {

    private LocalDateTime clickedAt;
    private String ipAddress;
    private String userAgent;
    private String referer;
    private String country;
    private String city;

}
