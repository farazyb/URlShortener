package com.example.urlshortener.model.repository;

import com.example.urlshortener.model.entity.URL;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.stereotype.Component;

@Component
public class UrlRepositoryImp {
    private final UrlRepository urlRepository;

    protected UrlRepositoryImp(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public URL save(URL url) {

        return urlRepository.save(url);
    }

    public URL findByShortUrl(String shortUrl) throws EntityNotFoundException {
        return urlRepository.findByShortUrl(shortUrl).orElseThrow(EntityNotFoundException::new);
    }
}
