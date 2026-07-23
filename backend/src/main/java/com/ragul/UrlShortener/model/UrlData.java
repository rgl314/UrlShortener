package com.ragul.UrlShortener.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UrlData {

    private String originalUrl;
    private String shortCode;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private int clickCount;
    private String createdBy;
    private boolean isActive;
    private List<ClickEvent> clickEvents;

}
