package com.example.urlshortener;

import com.example.urlshortener.model.entity.URL;
import com.example.urlshortener.model.repository.IdAllocator;
import com.example.urlshortener.model.repository.UrlRepositoryImp;
import com.example.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {
    @Mock
    private UrlRepositoryImp urlRepositoryImp;
    @Mock
    IdAllocator idAllocator;
    @InjectMocks
    private UrlService urlService;

    @Captor
    private ArgumentCaptor<URL> urlCaptor;


    @Test
    public void givenValidHttpURl_WhenSave_ThenReturnPersistedURLWithShortCode() {
        // given
        String originalUrl = "http://example.com/some/page";
        URL persisted = URL.builder()
                .id(1L)
                .originalUrl(originalUrl)
                .shortUrl("q0U")
                .build();

        given(idAllocator.nextId()).willReturn(100_000L);
        given(urlRepositoryImp.save(any(URL.class))).willReturn(persisted);

        // when
        URL result = urlService.save(originalUrl);

        // then
        then(urlRepositoryImp).should().save(urlCaptor.capture());
        URL toSave = urlCaptor.getValue();
        assertThat(toSave.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(toSave.getShortUrl()).isEqualTo("q0U");

        assertThat(result).isSameAs(persisted);
    }

    @Test
    public void givenValidHttpsURL_WhenSave_ThenReturnPersistedURLWithShortCode() {
        //given
        String originalUrl = "https://example.com/some/page";
        URL persisted = URL.builder()
                .id(1L)
                .originalUrl(originalUrl)
                .shortUrl("q0U")
                .build();
        given(idAllocator.nextId()).willReturn(100_000L);
        given(urlRepositoryImp.save(any(URL.class))).willReturn(persisted);
        //when

        URL result = urlService.save(originalUrl);
        //then
        then(urlRepositoryImp).should().save(urlCaptor.capture());
        URL toSave = urlCaptor.getValue();
        assertThat(toSave.getOriginalUrl()).isEqualTo(originalUrl);
        assertThat(toSave.getShortUrl()).isEqualTo("q0U");
        assertThat(result).isSameAs(persisted);
    }
    @Test
    public void givenBlankUrl_WhenSave_ThenThrowInvalid

    @Test
    void get() {
    }

    @Test
    void hasFunction() {
    }
}