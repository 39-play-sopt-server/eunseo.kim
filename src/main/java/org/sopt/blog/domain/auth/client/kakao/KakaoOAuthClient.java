package org.sopt.blog.domain.auth.client.kakao;

import lombok.RequiredArgsConstructor;
import org.sopt.blog.domain.auth.client.OAuthClient;
import org.sopt.blog.domain.auth.client.OAuthUser;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoTokenResponse;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoUserResponse;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.service.KakaoAuthorizationService;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private final KakaoTokenClient kakaoTokenClient;
    private final KakaoUserClient kakaoUserClient;
    private final KakaoAuthorizationService kakaoAuthorizationService;

    @Override
    public OAuthProvider provider() {
        return OAuthProvider.KAKAO;
    }

    @Override
    public URI createAuthorizationUri(String state) {
        return kakaoAuthorizationService.createAuthorizationUri(state);
    }

    @Override
    public OAuthUser getOAuthUser(String authorizationCode) {
        KakaoTokenResponse tokenResponse =
                kakaoTokenClient.exchangeAuthorizationCode(authorizationCode);

        KakaoUserResponse userResponse =
                kakaoUserClient.getUserInfo(tokenResponse.accessToken());

        return new OAuthUser(
                String.valueOf(userResponse.id()),
                extractNickname(userResponse)
        );
    }

    private String extractNickname(KakaoUserResponse userResponse) {
        if (userResponse.kakaoAccount() == null
                || userResponse.kakaoAccount().profile() == null
                || userResponse.kakaoAccount().profile().nickname() == null
                || userResponse.kakaoAccount().profile().nickname().isBlank()) {
            throw new IllegalStateException("카카오 닉네임을 확인할 수 없습니다.");
        }

        return userResponse.kakaoAccount().profile().nickname();
    }
}
