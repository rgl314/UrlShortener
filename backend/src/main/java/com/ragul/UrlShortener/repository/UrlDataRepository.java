package com.ragul.UrlShortener.repository;

import com.ragul.UrlShortener.model.UrlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlDataRepository extends JpaRepository<UrlData, Long> {
    Optional<UrlData> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);
}
