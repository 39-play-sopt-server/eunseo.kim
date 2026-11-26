package org.sopt.blog.domain.auth.controller;

import org.sopt.blog.domain.auth.controller.dto.request.LoginRequest;
import org.sopt.blog.domain.auth.controller.dto.request.SignupRequest;
import org.sopt.blog.domain.auth.controller.dto.response.LoginResponse;
import org.sopt.blog.domain.auth.service.AuthService;
import org.sopt.blog.domain.auth.service.TokenPair;
import org.sopt.blog.domain.auth.service.RefreshTokenCookieProvider;
import org.sopt.blog.domain.auth.service.TokenService;
import org.sopt.blog.domain.auth.service.OAuthStateCookieProvider;
import org.sopt.blog.domain.auth.service.CsrfTokenCookieProvider;
import org.sopt.blog.domain.auth.config.OAuthProperties;
import org.sopt.blog.domain.auth.entity.OAuthProvider;
import org.sopt.blog.domain.auth.service.OAuthLoginService;
import org.sopt.blog.global.common.exception.CustomException;
import org.sopt.blog.global.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final CsrfTokenCookieProvider csrfTokenCookieProvider;
    private final OAuthStateCookieProvider oauthStateCookieProvider;
    private final OAuthLoginService oauthLoginService;
    private final OAuthProperties oauthProperties;

    public AuthController(
            AuthService authService,
            TokenService tokenService,
            RefreshTokenCookieProvider refreshTokenCookieProvider,
            CsrfTokenCookieProvider csrfTokenCookieProvider,
            OAuthStateCookieProvider oauthStateCookieProvider,
            OAuthLoginService oauthLoginService,
            OAuthProperties oauthProperties
    ) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.refreshTokenCookieProvider = refreshTokenCookieProvider;
        this.csrfTokenCookieProvider = csrfTokenCookieProvider;
        this.oauthStateCookieProvider = oauthStateCookieProvider;
        this.oauthLoginService = oauthLoginService;
        this.oauthProperties = oauthProperties;
    }

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.ok(authService.signup(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return tokenResponse(authService.login(request.email(), request.password()));
    }

    @GetMapping("/{provider}")
    public ResponseEntity<Void> redirectToOAuth(@PathVariable String provider) {
        OAuthProvider oauthProvider = OAuthProvider.from(provider);
        String state = oauthStateCookieProvider.createState();
        ResponseCookie stateCookie = oauthStateCookieProvider.create(state);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        oauthLoginService.createAuthorizationUri(oauthProvider, state).toString()
                )
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .build();
    }

    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = "oauthState", required = false) String cookieState
    ) {
        ResponseCookie deletedStateCookie = oauthStateCookieProvider.delete();

        if (!oauthStateCookieProvider.matches(cookieState, state)) {
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.SET_COOKIE, deletedStateCookie.toString())
                    .build();
        }

        TokenPair tokenPair = oauthLoginService.login(OAuthProvider.from(provider), code);
        ResponseCookie refreshCookie = refreshTokenCookieProvider.create(
                tokenPair.refreshToken()
        );
        ResponseCookie csrfCookie = csrfTokenCookieProvider.create();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(
                        HttpHeaders.LOCATION,
                        oauthProperties.getSuccessRedirectUri()
                )
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deletedStateCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        String accessToken = authorizationHeader.substring("Bearer ".length());

        tokenService.logout(accessToken, refreshToken);

        ResponseCookie deletedRefreshCookie = refreshTokenCookieProvider.delete();
        ResponseCookie deletedCsrfCookie = csrfTokenCookieProvider.delete();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deletedRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deletedCsrfCookie.toString())
                .build();
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long memberId,
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        String accessToken = authorizationHeader.substring("Bearer ".length());

        authService.withdraw(memberId, accessToken, refreshToken);

        ResponseCookie deletedRefreshCookie = refreshTokenCookieProvider.delete();
        ResponseCookie deletedCsrfCookie = csrfTokenCookieProvider.delete();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deletedRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, deletedCsrfCookie.toString())
                .build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            @CookieValue(name = CsrfTokenCookieProvider.COOKIE_NAME, required = false)
            String csrfCookie,
            @RequestHeader(name = CsrfTokenCookieProvider.HEADER_NAME, required = false)
            String csrfHeader
    ) {
        if (!csrfTokenCookieProvider.matches(csrfCookie, csrfHeader)) {
            throw new CustomException(
                    ErrorCode.INVALID_CSRF_TOKEN,
                    "Refresh Token 재발급에 필요한 CSRF 토큰이 유효하지 않습니다."
            );
        }
        return tokenResponse(tokenService.reissue(refreshToken));
    }

    private ResponseEntity<LoginResponse> tokenResponse(TokenPair tokenPair) {
        ResponseCookie refreshCookie =
                refreshTokenCookieProvider.create(tokenPair.refreshToken());
        ResponseCookie csrfCookie = csrfTokenCookieProvider.create();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + tokenPair.accessToken()
                )
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, csrfCookie.toString())
                .body(new LoginResponse(tokenPair.accessToken()));
    }
}
