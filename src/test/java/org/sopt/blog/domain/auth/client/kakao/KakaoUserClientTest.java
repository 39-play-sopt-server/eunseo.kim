package org.sopt.blog.domain.auth.client.kakao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoUserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoUserClientTest {

    private KakaoUserClient kakaoUserClient;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        kakaoUserClient = new KakaoUserClient(restClientBuilder);
    }

    @Test
    void 카카오_Access_Token으로_사용자_정보를_조회한다() {
        mockServer.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer kakao-access-token"
                ))
                .andRespond(withSuccess(
                        """
                        {
                          "id": 123456789,
                          "kakao_account": {
                            "profile": {
                              "nickname": "카카오닉네임"
                            }
                          }
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        KakaoUserResponse response = kakaoUserClient
                .getUserInfo("kakao-access-token");

        assertEquals(123456789L, response.id());
        assertEquals("카카오닉네임", response.kakaoAccount().profile().nickname());
        mockServer.verify();
    }
}
