package com.ragul.UrlShortener.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Builder
@Entity
@Table(name = "url_data")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UrlData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false)
    private String originalUrl;

    @Column(name = "short_code", unique = true, nullable = false)
    private String shortCode;

    @Column(name = "click_count")
    private int clickCount;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "is_active")
    private boolean isActive;

    @OneToMany(mappedBy = "urlData", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClickEvent> clickEvents = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

}
