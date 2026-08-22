package com.example.urlshortener.model.repository;

import com.example.urlshortener.model.entity.URL;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends CrudRepository<URL, Long> {
    Optional<URL> findByShortUrl(String shortUrl);
}
