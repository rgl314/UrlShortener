package com.ragul.UrlShortener.model;

import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RateLimitData {

    private int minuteCount;
    private int hourCount;

    private LocalDateTime minuteWindowStart;
    private LocalDateTime hourWindowStart;

}
