package org.sopt.blog.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.config.OAuthStateCookieProperties;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuthStateCookieProviderTest {

    private OAuthStateCookieProvider oauthStateCookieProvider;

    @BeforeEach
    void setUp() {
        OAuthStateCookieProperties properties = new OAuthStateCookieProperties();
        properties.setName("oauthState");
        properties.setPath("/auth/kakao");
        properties.setSecure(true);
        properties.setSameSite("Lax");
        properties.setMaxAgeSeconds(300);

        oauthStateCookieProvider = new OAuthStateCookieProvider(properties);
    }

    @Test
    void 매번_서로_다른_state를_생성한다() {
        String firstState = oauthStateCookieProvider.createState();
        String secondState = oauthStateCookieProvider.createState();

        assertNotEquals(firstState, secondState);
        assertTrue(oauthStateCookieProvider.matches(firstState, firstState));
    }

    @Test
    void state를_HttpOnly_Lax_쿠키로_생성한다() {
        ResponseCookie cookie = oauthStateCookieProvider.create("state-value");

        assertEquals("oauthState", cookie.getName());
        assertEquals("state-value", cookie.getValue());
        assertEquals("/auth/kakao", cookie.getPath());
        assertEquals("Lax", cookie.getSameSite());
        assertEquals(Duration.ofSeconds(300), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }

    @Test
    void 쿠키와_콜백의_state가_다르거나_없으면_실패한다() {
        assertFalse(oauthStateCookieProvider.matches("cookie-state", "callback-state"));
        assertFalse(oauthStateCookieProvider.matches(null, "callback-state"));
        assertFalse(oauthStateCookieProvider.matches("cookie-state", null));
    }

    @Test
    void state_쿠키를_삭제한다() {
        ResponseCookie cookie = oauthStateCookieProvider.delete();

        assertEquals("oauthState", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
    }
}
