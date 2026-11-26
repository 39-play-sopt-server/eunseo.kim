package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.config.KakaoProperties;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KakaoAuthorizationServiceTest {

    private KakaoAuthorizationService kakaoAuthorizationService;

    @BeforeEach
    void setUp() {
        KakaoProperties kakaoProperties = new KakaoProperties();
        kakaoProperties.setRestApiKey("test-rest-api-key");
        kakaoProperties.setClientSecret("test-client-secret");
        kakaoProperties.setRedirectUri("http://localhost:8080/auth/kakao/callback");

        kakaoAuthorizationService = new KakaoAuthorizationService(kakaoProperties);
    }

    @Test
    void 카카오_인가_URL에_필수_파라미터를_포함한다() {
        URI authorizationUri = kakaoAuthorizationService
                .createAuthorizationUri("safe-state-value");
        String decodedUri = URLDecoder.decode(
                authorizationUri.toString(),
                StandardCharsets.UTF_8
        );

        assertEquals("https", authorizationUri.getScheme());
        assertEquals("kauth.kakao.com", authorizationUri.getHost());
        assertEquals("/oauth/authorize", authorizationUri.getPath());
        assertTrue(decodedUri.contains("response_type=code"));
        assertTrue(decodedUri.contains("client_id=test-rest-api-key"));
        assertTrue(decodedUri.contains(
                "redirect_uri=http://localhost:8080/auth/kakao/callback"
        ));
        assertTrue(decodedUri.contains("state=safe-state-value"));
    }

    @Test
    void 카카오_인가_URL에_Client_Secret을_노출하지_않는다() {
        URI authorizationUri = kakaoAuthorizationService
                .createAuthorizationUri("safe-state-value");

        assertFalse(authorizationUri.toString().contains("client_secret"));
        assertFalse(authorizationUri.toString().contains("test-client-secret"));
    }
}
