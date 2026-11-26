package org.sopt.blog.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class OAuthPropertiesIntegrationTest {

    @Autowired
    private OAuthProperties oauthProperties;

    @Test
    void 테스트_프로필의_소셜_로그인_성공_리다이렉트_주소를_바인딩한다() {
        assertEquals(
                "http://localhost:3000",
                oauthProperties.getSuccessRedirectUri()
        );
    }
}
