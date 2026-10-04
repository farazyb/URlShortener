package com.example.urlshortener;

import com.example.urlshortener.model.entity.URL;
import com.example.urlshortener.model.repository.IdAllocator;
import com.example.urlshortener.model.repository.UrlRepositoryImp;
import com.example.urlshortener.service.UrlService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.InvalidUrlException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
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

    @ParameterizedTest(name = "Given blank url [{0}], when save is called, then InvalidUrlException is thrown")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n", "\t", "\r"})
    public void givenBlankUrl_WhenSave_ThenThrowInvalidUrlException(String blank) {
        //given
        String url = blank;
        //when
        Executable executable = () ->urlService.save(url);

        //then
        Exception exception=assertThrows(InvalidUrlException.class, executable);
        assertEquals("URL must not be blank", exception.getMessage());
        then(urlRepositoryImp).shouldHaveNoInteractions();
        then(idAllocator).shouldHaveNoInteractions();

    }
    @Nested
    @DisplayName("Blank input")
    class BlankInput {

        @ParameterizedTest(name = "Given blank url [{0}], when save is called, then \"must not be blank\" is thrown")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\n", "\t", "\r", "   \t \n  "})
        void givenBlankUrl_whenSave_thenThrowsBlankMessage(String blankUrl) {
            InvalidUrlException ex = assertThrows(InvalidUrlException.class,
                    () -> urlService.save(blankUrl));

            assertEquals("URL must not be blank", ex.getMessage());
            verifyNoInteractions(idAllocator, urlRepositoryImp);
        }
    }

    @Nested
    @DisplayName("Syntactically malformed input (URISyntaxException)")
    class MalformedInput {

        @ParameterizedTest(name = "Given malformed url [{0}], when save is called, then \"Malformed URL\" is thrown")
        @ValueSource(strings = {
                "http://exa mple.com",       // illegal space in host
                "http://example.com/pa th",  // illegal space in path
                "http://[::1",               // unclosed IPv6 literal bracket
                "http://example.com/%",      // incomplete percent-encoding
                "http://example.com/%zz",    // invalid percent-encoding (not hex)
                "ht tp://example.com",       // illegal space in scheme
                "http://"                    // empty authority is syntactically illegal
        })
        void givenMalformedUrl_whenSave_thenThrowsMalformedMessage(String malformedUrl) {
            InvalidUrlException ex = assertThrows(InvalidUrlException.class,
                    () -> urlService.save(malformedUrl));

            assertEquals("Malformed URL", ex.getMessage());
            verifyNoInteractions(idAllocator, urlRepositoryImp);
        }
    }

    @Nested
    @DisplayName("Missing or disallowed scheme (parses fine, scheme check fails)")
    class SchemeValidation {

        @ParameterizedTest(name = "Given url with missing/disallowed scheme [{0}], when save is called, then validation message is thrown")
        @ValueSource(strings = {
                "example.com",             // no scheme -> getScheme() == null
                "//example.com/path",      // network-path reference -> scheme == null
                "ftp://example.com",       // disallowed scheme
                "javascript:alert(1)",     // disallowed scheme (would be an XSS vector if allowed)
                "file:///etc/passwd",      // disallowed scheme
                "mailto:test@example.com", // disallowed scheme
                "gopher://example.com"     // disallowed scheme
        })
        void givenInvalidScheme_whenSave_thenThrowsValidationMessage(String url) {
            InvalidUrlException ex = assertThrows(InvalidUrlException.class,
                    () -> urlService.save(url));

            assertEquals("URL must be a valid http:// or https:// address", ex.getMessage());
            verifyNoInteractions(idAllocator, urlRepositoryImp);
        }

        @ParameterizedTest(name = "Given scheme case variant [{0}], when save is called, then it is accepted (case-insensitive check)")
        @ValueSource(strings = {"HTTP://example.com", "HtTpS://example.com", "HTTPS://example.com"})
        void givenMixedCaseScheme_whenSave_thenAccepted(String url) {
            given(idAllocator.nextId()).willReturn(1L);
            given(urlRepositoryImp.save(any(URL.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            urlService.save(url);

            ArgumentCaptor<URL> captor = ArgumentCaptor.forClass(URL.class);
            verify(urlRepositoryImp).save(captor.capture());
            assertEquals(url, captor.getValue().getOriginalUrl());
        }
    }

    @Nested
    @DisplayName("Missing host (parses fine, scheme valid, host == null)")
    class HostValidation {

        @ParameterizedTest(name = "Given url with missing host [{0}], when save is called, then validation message is thrown")
        @ValueSource(strings = {
                "http:example.com",         // opaque URI, no authority component -> host == null
                "http:///path",             // authority present but empty -> host == null
                "http:///",                 // same, no path either
                "http://256.256.256.256"    // NOT range-validated: java.net.URI just can't parse it
                // as a host, so getHost() == null. Confirmed empirically.
        })
        void givenMissingHost_whenSave_thenThrowsValidationMessage(String url) {
            InvalidUrlException ex = assertThrows(InvalidUrlException.class,
                    () -> urlService.save(url));

            assertEquals("URL must be a valid http:// or https:// address", ex.getMessage());
            verifyNoInteractions(idAllocator, urlRepositoryImp);
        }
    }

    @Nested
    @DisplayName("Valid input")
    class ValidInput {

        @ParameterizedTest(name = "Given valid url [{0}], when save is called, then it is persisted unchanged")
        @ValueSource(strings = {
                "http://example.com",
                "https://example.com",
                "https://example.com/path?query=1#fragment",
                "https://sub.example.com:8443/path",
                "http://192.168.1.1/path"
        })
        void givenValidUrl_whenSave_thenPersistsNormalizedUrl(String url) {
            given(idAllocator.nextId()).willReturn(42L);
            given(urlRepositoryImp.save(any(URL.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            URL result = urlService.save(url);

            ArgumentCaptor<URL> captor = ArgumentCaptor.forClass(URL.class);
            verify(urlRepositoryImp).save(captor.capture());
            assertEquals(url, captor.getValue().getOriginalUrl());
            assertEquals(urlService.hashFunction(42L), result.getShortUrl());
        }

        @ParameterizedTest(name = "Given url with surrounding whitespace [{0}], when save is called, then whitespace is trimmed")
        @ValueSource(strings = {
                "  http://example.com  ",
                "\thttp://example.com\n",
                " https://example.com/path "
        })
        void givenUrlWithSurroundingWhitespace_whenSave_thenTrimmed(String url) {
            given(idAllocator.nextId()).willReturn(7L);
            given(urlRepositoryImp.save(any(URL.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            urlService.save(url);

            ArgumentCaptor<URL> captor = ArgumentCaptor.forClass(URL.class);
            verify(urlRepositoryImp).save(captor.capture());
            assertEquals(url.trim(), captor.getValue().getOriginalUrl());
        }
    }

    @Test
    void get() {
    }

    @Test
    void hasFunction() {
    }
}