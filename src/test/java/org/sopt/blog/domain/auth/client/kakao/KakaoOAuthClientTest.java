package org.sopt.blog.domain.auth.client.kakao;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.client.OAuthUser;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoTokenResponse;
import org.sopt.blog.domain.auth.client.kakao.dto.KakaoUserResponse;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.service.KakaoAuthorizationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KakaoOAuthClientTest {

    @Test
    void 인가_코드로_카카오_사용자를_공통_OAuthUser로_변환한다() {
        KakaoTokenClient kakaoTokenClient = mock(KakaoTokenClient.class);
        KakaoUserClient kakaoUserClient = mock(KakaoUserClient.class);
        KakaoOAuthClient kakaoOAuthClient = new KakaoOAuthClient(
                kakaoTokenClient,
                kakaoUserClient,
                mock(KakaoAuthorizationService.class)
        );
        KakaoTokenResponse tokenResponse = new KakaoTokenResponse("kakao-access-token");
        KakaoUserResponse userResponse = new KakaoUserResponse(
                123456789L,
                new KakaoUserResponse.KakaoAccount(
                        new KakaoUserResponse.Profile("카카오닉네임")
                )
        );

        when(kakaoTokenClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(tokenResponse);
        when(kakaoUserClient.getUserInfo("kakao-access-token"))
                .thenReturn(userResponse);

        OAuthUser oauthUser = kakaoOAuthClient.getOAuthUser("authorization-code");

        assertEquals(OAuthProvider.KAKAO, kakaoOAuthClient.provider());
        assertEquals("123456789", oauthUser.providerUserId());
        assertEquals("카카오닉네임", oauthUser.nickname());
        verify(kakaoTokenClient).exchangeAuthorizationCode("authorization-code");
        verify(kakaoUserClient).getUserInfo("kakao-access-token");
    }

    @Test
    void 닉네임이_없으면_소셜_로그인을_중단한다() {
        KakaoTokenClient kakaoTokenClient = mock(KakaoTokenClient.class);
        KakaoUserClient kakaoUserClient = mock(KakaoUserClient.class);
        KakaoOAuthClient kakaoOAuthClient = new KakaoOAuthClient(
                kakaoTokenClient,
                kakaoUserClient,
                mock(KakaoAuthorizationService.class)
        );

        when(kakaoTokenClient.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new KakaoTokenResponse("kakao-access-token"));
        when(kakaoUserClient.getUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserResponse(123456789L, null));

        assertThrows(
                IllegalStateException.class,
                () -> kakaoOAuthClient.getOAuthUser("authorization-code")
        );
    }
}
