package org.sopt.blog.domain.auth.controller;

import org.junit.jupiter.api.Test;
import org.sopt.blog.domain.auth.config.OAuthProperties;
import org.sopt.blog.domain.auth.controller.dto.request.LoginRequest;
import org.sopt.blog.domain.auth.controller.dto.response.LoginResponse;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.service.AuthService;
import org.sopt.blog.domain.auth.service.CsrfTokenCookieProvider;
import org.sopt.blog.domain.auth.service.OAuthLoginService;
import org.sopt.blog.domain.auth.service.OAuthStateCookieProvider;
import org.sopt.blog.domain.auth.service.RefreshTokenCookieProvider;
import org.sopt.blog.domain.auth.service.TokenPair;
import org.sopt.blog.domain.auth.service.TokenService;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    @Test
    void 로그인_성공시_Access_Token은_body로_Refresh_Token은_쿠키로_응답한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        AuthController authController = createAuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider
        );
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "refresh-token")
                .httpOnly(true)
                .path("/auth")
                .build();

        when(authService.login("eunseo@example.com", "password"))
                .thenReturn(tokenPair);
        when(refreshTokenCookieProvider.create("refresh-token"))
                .thenReturn(refreshCookie);

        ResponseEntity<LoginResponse> response = authController.login(
                new LoginRequest("eunseo@example.com", "password")
        );

        assertEquals("access-token", response.getBody().accessToken());
        assertEquals(
                "Bearer access-token",
                response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)
        );
        assertEquals(refreshCookie.toString(), response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void 재발급_성공시_새_Access_Token은_body로_새_Refresh_Token은_쿠키로_응답한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        AuthController authController = createAuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider
        );
        TokenPair tokenPair = new TokenPair("new-access-token", "new-refresh-token");
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "new-refresh-token")
                .httpOnly(true)
                .path("/auth")
                .build();

        when(tokenService.reissue("old-refresh-token"))
                .thenReturn(tokenPair);
        when(refreshTokenCookieProvider.create("new-refresh-token"))
                .thenReturn(refreshCookie);

        ResponseEntity<LoginResponse> response = authController.reissue(
                "old-refresh-token",
                "csrf-token",
                "csrf-token"
        );

        assertEquals("new-access-token", response.getBody().accessToken());
        assertEquals(
                "Bearer new-access-token",
                response.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)
        );
        assertEquals(refreshCookie.toString(), response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
    }

    @Test
    void CSRF_헤더가_쿠키와_다르면_재발급하지_않는다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        CsrfTokenCookieProvider csrfTokenCookieProvider = mock(CsrfTokenCookieProvider.class);
        AuthController authController = new AuthController(
                authService,
                tokenService,
                mock(RefreshTokenCookieProvider.class),
                csrfTokenCookieProvider,
                mock(OAuthStateCookieProvider.class),
                mock(OAuthLoginService.class),
                oauthProperties()
        );

        when(csrfTokenCookieProvider.matches("csrf-cookie", "invalid-header"))
                .thenReturn(false);

        CustomException exception = assertThrows(
                CustomException.class,
                () -> authController.reissue(
                        "refresh-token",
                        "csrf-cookie",
                        "invalid-header"
                )
        );

        assertEquals(ErrorCode.INVALID_CSRF_TOKEN, exception.getErrorCode());
        verifyNoInteractions(tokenService);
    }

    @Test
    void 로그아웃하면_Access_Token과_Refresh_Token을_폐기하고_만료_쿠키를_응답한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        AuthController authController = createAuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider
        );
        ResponseCookie deletedRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();

        when(refreshTokenCookieProvider.delete()).thenReturn(deletedRefreshCookie);

        ResponseEntity<Void> response = authController.logout(
                "Bearer access-token",
                "refresh-token"
        );

        verify(tokenService).logout("access-token", "refresh-token");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(
                deletedRefreshCookie.toString(),
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)
        );
    }

    @Test
    void 회원_탈퇴하면_인증된_회원과_토큰을_전달하고_만료_쿠키를_응답한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        AuthController authController = createAuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider
        );
        ResponseCookie deletedRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();

        when(refreshTokenCookieProvider.delete()).thenReturn(deletedRefreshCookie);

        ResponseEntity<Void> response = authController.withdraw(
                1L,
                "Bearer access-token",
                "refresh-token"
        );

        verify(authService).withdraw(1L, "access-token", "refresh-token");
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        assertEquals(
                deletedRefreshCookie.toString(),
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)
        );
    }

    @Test
    void 카카오_로그인_시작시_state_쿠키를_설정하고_카카오로_리다이렉트한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        OAuthStateCookieProvider oauthStateCookieProvider = mock(OAuthStateCookieProvider.class);
        OAuthLoginService oauthLoginService = mock(OAuthLoginService.class);
        AuthController authController = new AuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider,
                csrfTokenCookieProvider(),
                oauthStateCookieProvider,
                oauthLoginService,
                oauthProperties()
        );
        ResponseCookie stateCookie = ResponseCookie.from("oauthState", "state-value")
                .httpOnly(true)
                .path("/auth")
                .sameSite("Lax")
                .build();
        URI authorizationUri = URI.create(
                "https://kauth.kakao.com/oauth/authorize?state=state-value"
        );

        when(oauthStateCookieProvider.createState()).thenReturn("state-value");
        when(oauthStateCookieProvider.create("state-value")).thenReturn(stateCookie);
        when(oauthLoginService.createAuthorizationUri(OAuthProvider.KAKAO, "state-value"))
                .thenReturn(authorizationUri);

        ResponseEntity<Void> response = authController.redirectToOAuth("kakao");

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(authorizationUri.toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
        assertEquals(stateCookie.toString(), response.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
        verify(oauthStateCookieProvider).createState();
        verify(oauthStateCookieProvider).create("state-value");
        verify(oauthLoginService).createAuthorizationUri(OAuthProvider.KAKAO, "state-value");
    }

    @Test
    void 카카오_콜백이_유효하면_Refresh_Token_쿠키를_설정하고_프론트로_이동한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        OAuthStateCookieProvider oauthStateCookieProvider = mock(OAuthStateCookieProvider.class);
        OAuthLoginService oauthLoginService = mock(OAuthLoginService.class);
        OAuthProperties oauthProperties = oauthProperties();
        AuthController authController = new AuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider,
                csrfTokenCookieProvider(),
                oauthStateCookieProvider,
                oauthLoginService,
                oauthProperties
        );
        ResponseCookie deletedStateCookie = ResponseCookie.from("oauthState", "")
                .httpOnly(true)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "refresh-token")
                .httpOnly(true)
                .path("/auth")
                .build();
        TokenPair tokenPair = new TokenPair("access-token", "refresh-token");

        when(oauthStateCookieProvider.delete()).thenReturn(deletedStateCookie);
        when(oauthStateCookieProvider.matches("cookie-state", "callback-state"))
                .thenReturn(true);
        when(oauthLoginService.login(OAuthProvider.KAKAO, "authorization-code"))
                .thenReturn(tokenPair);
        when(refreshTokenCookieProvider.create("refresh-token"))
                .thenReturn(refreshCookie);

        ResponseEntity<Void> response = authController.oauthCallback(
                "kakao",
                "authorization-code",
                "callback-state",
                "cookie-state"
        );

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(
                "http://localhost:3000",
                response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE)
                .contains(refreshCookie.toString()));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE)
                .contains(deletedStateCookie.toString()));
        verify(oauthLoginService).login(OAuthProvider.KAKAO, "authorization-code");
    }

    @Test
    void 카카오_콜백_state가_다르면_로그인하지_않고_400을_응답한다() {
        AuthService authService = mock(AuthService.class);
        TokenService tokenService = mock(TokenService.class);
        RefreshTokenCookieProvider refreshTokenCookieProvider = mock(RefreshTokenCookieProvider.class);
        OAuthStateCookieProvider oauthStateCookieProvider = mock(OAuthStateCookieProvider.class);
        OAuthLoginService oauthLoginService = mock(OAuthLoginService.class);
        AuthController authController = new AuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider,
                csrfTokenCookieProvider(),
                oauthStateCookieProvider,
                oauthLoginService,
                oauthProperties()
        );
        ResponseCookie deletedStateCookie = ResponseCookie.from("oauthState", "")
                .httpOnly(true)
                .path("/auth")
                .maxAge(Duration.ZERO)
                .build();

        when(oauthStateCookieProvider.delete()).thenReturn(deletedStateCookie);
        when(oauthStateCookieProvider.matches("wrong-cookie-state", "callback-state"))
                .thenReturn(false);

        ResponseEntity<Void> response = authController.oauthCallback(
                "kakao",
                "authorization-code",
                "callback-state",
                "wrong-cookie-state"
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(
                deletedStateCookie.toString(),
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)
        );
        verifyNoInteractions(oauthLoginService, refreshTokenCookieProvider);
    }

    private AuthController createAuthController(
            AuthService authService,
            TokenService tokenService,
            RefreshTokenCookieProvider refreshTokenCookieProvider
    ) {
        return new AuthController(
                authService,
                tokenService,
                refreshTokenCookieProvider,
                csrfTokenCookieProvider(),
                mock(OAuthStateCookieProvider.class),
                mock(OAuthLoginService.class),
                oauthProperties()
        );
    }

    private OAuthProperties oauthProperties() {
        OAuthProperties oauthProperties = new OAuthProperties();
        oauthProperties.setSuccessRedirectUri("http://localhost:3000");
        return oauthProperties;
    }

    private CsrfTokenCookieProvider csrfTokenCookieProvider() {
        CsrfTokenCookieProvider provider = mock(CsrfTokenCookieProvider.class);
        ResponseCookie cookie = ResponseCookie.from("XSRF-TOKEN", "csrf-token")
                .path("/auth")
                .sameSite("Strict")
                .build();
        ResponseCookie deletedCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .path("/auth")
                .sameSite("Strict")
                .maxAge(Duration.ZERO)
                .build();

        when(provider.create()).thenReturn(cookie);
        when(provider.delete()).thenReturn(deletedCookie);
        when(provider.matches(anyString(), anyString())).thenReturn(true);
        return provider;
    }
}
