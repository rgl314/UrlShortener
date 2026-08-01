package com.ragul.UrlShortener.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "click_events")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_data_id", nullable = false)
    private UrlData urlData;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;  // which browser it occurred

    @Column(name = "referer")
    private String referer;   // where it came from

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

}
