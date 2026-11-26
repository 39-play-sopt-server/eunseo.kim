package org.sopt.blog.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class OAuthStateCookiePropertiesIntegrationTest {

    @Autowired
    private OAuthStateCookieProperties oauthStateCookieProperties;

    @Test
    void 테스트_프로필의_OAuth_state_쿠키_설정을_바인딩한다() {
        assertEquals("oauthState", oauthStateCookieProperties.getName());
        assertEquals("/auth", oauthStateCookieProperties.getPath());
        assertFalse(oauthStateCookieProperties.isSecure());
        assertEquals("Lax", oauthStateCookieProperties.getSameSite());
        assertEquals(300, oauthStateCookieProperties.getMaxAgeSeconds());
    }
}
