package com.ragul.UrlShortener.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClickEvent {

    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;  // which browser it occurred
    private String referer;   // where it came from
    private String country;
    private String city;

}
