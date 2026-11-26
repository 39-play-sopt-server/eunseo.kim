package org.sopt.blog.domain.auth.client.kakao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoTokenResponse;
import org.sopt.blog.domain.auth.config.KakaoProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoTokenClientTest {

    private KakaoTokenClient kakaoTokenClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        KakaoProperties kakaoProperties = new KakaoProperties();
        kakaoProperties.setRestApiKey("test-rest-api-key");
        kakaoProperties.setClientSecret("test-client-secret");
        kakaoProperties.setRedirectUri("http://localhost:8080/auth/kakao/callback");

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        kakaoTokenClient = new KakaoTokenClient(kakaoProperties, restClientBuilder);
    }

    @Test
    void 인가_코드로_카카오_Access_Token을_요청한다() {
        mockServer.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_FORM_URLENCODED
                ))
                .andExpect(content().formDataContains(Map.of(
                        "grant_type", "authorization_code",
                        "client_id", "test-rest-api-key",
                        "redirect_uri", "http://localhost:8080/auth/kakao/callback",
                        "code", "authorization-code",
                        "client_secret", "test-client-secret"
                )))
                .andRespond(withSuccess(
                        "{\"access_token\":\"kakao-access-token\"}",
                        MediaType.APPLICATION_JSON
                ));

        KakaoTokenResponse response = kakaoTokenClient
                .exchangeAuthorizationCode("authorization-code");

        assertEquals("kakao-access-token", response.accessToken());
        mockServer.verify();
    }
}
