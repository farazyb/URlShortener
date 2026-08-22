package com.example.urlshortener.service;


import com.example.urlshortener.service.dtos.UrlView;
import com.example.urlshortener.model.entity.URL;
import com.example.urlshortener.model.repository.IdAllocator;
import com.example.urlshortener.model.repository.UrlRepositoryImp;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.InvalidUrlException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

import static com.example.urlshortener.config.RedisConfig.*;

@Service
public class UrlService {

    private final UrlRepositoryImp urlRepositoryImp;
    private final IdAllocator idAllocator;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public UrlService(UrlRepositoryImp urlRepositoryImp, IdAllocator idAllocator) {
        this.urlRepositoryImp = urlRepositoryImp;
        this.idAllocator = idAllocator;
    }

    @Transactional()
    public URL save(String originalUrl) {
        String normalized = validateAndNormalize(originalUrl);
        Long id = idAllocator.nextId();
        return urlRepositoryImp.save(URL.builder()
                .originalUrl(normalized)
                .shortUrl(hashFunction(id))
                .build());
    }

    private String validateAndNormalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("URL must not be blank");
        }
        String trimmed = rawUrl.trim();

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("Malformed URL");
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
                || host == null || host.isBlank()) {
            throw new InvalidUrlException("URL must be a valid http:// or https:// address");
        }

        return trimmed;
    }


    @Cacheable(cacheNames = Caches.SHORT_URLS, key = "#shortURL", sync = true)
    public UrlView get(String shortURL) {
        return new UrlView(urlRepositoryImp.findByShortUrl(shortURL).getOriginalUrl());
    }

    public String hashFunction(Long id) {
        final String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(characters.charAt((int) (id % characters.length())));
            id /= characters.length();
        }
        return sb.reverse().toString();
    }

    @CacheEvict(cacheNames = Caches.SHORT_URLS, key = "#shortUrl")
    public void evict(String shortUrl) {
        // body intentionally empty - the annotation is the whole point
    }
}

