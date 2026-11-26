package org.sopt.blog.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class KakaoPropertiesIntegrationTest {

    @Autowired
    private KakaoProperties kakaoProperties;

    @Test
    void 테스트_프로필의_카카오_설정을_바인딩한다() {
        assertEquals("test-rest-api-key", kakaoProperties.getRestApiKey());
        assertEquals("test-client-secret", kakaoProperties.getClientSecret());
        assertEquals(
                "http://localhost:8080/auth/kakao/callback",
                kakaoProperties.getRedirectUri()
        );
    }
}
