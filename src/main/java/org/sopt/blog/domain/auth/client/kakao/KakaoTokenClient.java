package org.sopt.blog.domain.auth.client.kakao;

import org.sopt.blog.domain.auth.client.kakao.dto.KakaoTokenResponse;
import org.sopt.blog.domain.auth.config.KakaoProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoTokenClient {

    private static final String KAKAO_TOKEN_URI =
            "https://kauth.kakao.com/oauth/token";

    private final KakaoProperties kakaoProperties;
    private final RestClient restClient;

    public KakaoTokenClient(
            KakaoProperties kakaoProperties,
            RestClient.Builder restClientBuilder
    ) {
        this.kakaoProperties = kakaoProperties;
        this.restClient = restClientBuilder.build();
    }

    public KakaoTokenResponse exchangeAuthorizationCode(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoProperties.getRestApiKey());
        formData.add("redirect_uri", kakaoProperties.getRedirectUri());
        formData.add("code", code);
        formData.add("client_secret", kakaoProperties.getClientSecret());

        KakaoTokenResponse response = restClient.post()
                .uri(KAKAO_TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null
                || response.accessToken() == null
                || response.accessToken().isBlank()) {
            throw new IllegalStateException("카카오 Access Token 발급에 실패했습니다.");
        }

        return response;
    }
}
