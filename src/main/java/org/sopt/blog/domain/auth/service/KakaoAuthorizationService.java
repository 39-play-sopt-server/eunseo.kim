package org.sopt.blog.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.auth.config.KakaoProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class KakaoAuthorizationService {

    private static final String KAKAO_AUTHORIZATION_URI =
            "https://kauth.kakao.com/oauth/authorize";

    private final KakaoProperties kakaoProperties;

    public URI createAuthorizationUri(String state) {
        return UriComponentsBuilder
                .fromUriString(KAKAO_AUTHORIZATION_URI)
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoProperties.getRestApiKey())
                .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();
    }
}
