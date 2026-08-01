package com.ragul.UrlShortener.repository;

import com.ragul.UrlShortener.model.ClickEvent;
import com.ragul.UrlShortener.model.UrlData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {
    List<ClickEvent> findAllByUrlData(UrlData urlData);
}
