package com.example.urlshortener.model.repository;

import com.example.urlshortener.model.entity.URL;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface IdAllocator extends Repository<URL, Long> {
    @Query(value = "select nextval('url_sequence')", nativeQuery = true)
    long nextId();
}
