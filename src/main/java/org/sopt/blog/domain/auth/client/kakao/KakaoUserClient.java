package org.sopt.blog.domain.auth.client.kakao;

import org.sopt.blog.domain.auth.client.kakao.dto.KakaoUserResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class KakaoUserClient {

    private static final String KAKAO_USER_INFO_URI =
            "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;

    public KakaoUserClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public KakaoUserResponse getUserInfo(String kakaoAccessToken) {
        KakaoUserResponse response = restClient.get()
                .uri(KAKAO_USER_INFO_URI)
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + kakaoAccessToken
                )
                .retrieve()
                .body(KakaoUserResponse.class);

        if (response == null || response.id() == null) {
            throw new IllegalStateException("카카오 사용자 정보 조회에 실패했습니다.");
        }

        return response;
    }
}
