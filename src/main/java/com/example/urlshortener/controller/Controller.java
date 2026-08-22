package com.example.urlshortener.controller;

import com.example.urlshortener.service.UrlService;
import com.example.urlshortener.model.entity.URL;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api")
public class Controller {

    private final UrlService urlService;

    public Controller(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<URL> createUrl(@RequestParam("url") @NotBlank(message = "url must not be blank")
                                         @Pattern(regexp = "^https?://.+",
                                                 message = "url must start with http:// or https://")
                                             String originalUrl) {
        return ResponseEntity.ok((urlService.save(originalUrl)));
    }

    @GetMapping("/{shortURL:[A-Za-z0-9]{3,8}}")
    public RedirectView getUrl(@PathVariable("shortURL") String shortURL) {
        RedirectView view = new RedirectView(urlService.get(shortURL).originalUrl());
        view.setStatusCode(HttpStatus.FOUND);
        return view;
    }
}
