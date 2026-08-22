package com.example.urlshortener.service.dtos;

/**
 * What actually goes into Redis for the redirect hot path.
 *
 * Serializes to {"originalUrl":"https://..."} - about 40 bytes plus the URL,
 * versus a few hundred for the full entity with type metadata.
 *
 * NOT_FOUND is a negative-cache marker. It serializes to {"originalUrl":null},
 * which a typed JacksonJsonRedisSerializer round-trips fine, unlike Spring's
 * internal NullValue class.
 *
 * Records need the -parameters compiler flag for Jackson to bind constructor
 * arguments. The Spring Boot Maven and Gradle plugins set it by default; if you
 * build outside them, add it yourself or you get "Cannot construct instance".
 */
public record UrlView(String originalUrl) {

    public static final UrlView NOT_FOUND = new UrlView(null);

    public boolean isFound() {
        return originalUrl != null;
    }
}
