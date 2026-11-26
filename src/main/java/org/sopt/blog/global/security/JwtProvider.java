package org.sopt.blog.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtBuilder;

import org.springframework.stereotype.Component;
import org.sopt.blog.domain.member.entity.MemberRole;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ROLE_CLAIM = "role";
    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtProperties.getSecret())
        );
    }

    public String createAccessToken(Long memberId, MemberRole role) {
        return createToken(
                memberId,
                TokenType.ACCESS,
                role,
                jwtProperties.getAccessTokenExpiration()
        );
    }

    public String createRefreshToken(Long memberId) {
        return createToken(
                memberId,
                TokenType.REFRESH,
                null,
                jwtProperties.getRefreshTokenExpiration()
        );
    }

    private String createToken(
            Long memberId,
            TokenType tokenType,
            MemberRole role,
            long expiration
    ) {
        Date now = new Date();

        JwtBuilder builder = Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(memberId))
                .id(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration));

        if (role != null) {
            builder.claim(ROLE_CLAIM, role.name());
        }

        return builder
                .signWith(secretKey)
                .compact();
    }

    public TokenPayload parseAccessToken(String token) {
        return parseToken(token, TokenType.ACCESS);
    }

    public TokenPayload parseRefreshToken(String token) {
        return parseToken(token, TokenType.REFRESH);
    }

    private TokenPayload parseToken(String token, TokenType expectedTokenType) {
        Claims claims = Jwts.parser()
                .requireIssuer(jwtProperties.getIssuer())
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        TokenType actualTokenType = getTokenType(claims);

        if (actualTokenType != expectedTokenType) {
            throw new IllegalArgumentException("토큰 타입이 올바르지 않습니다.");
        }

        MemberRole role = actualTokenType == TokenType.ACCESS
                ? getRole(claims)
                : null;

        return new TokenPayload(
                Long.valueOf(claims.getSubject()),
                claims.getId(),
                actualTokenType,
                role,
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        );
    }

    private TokenType getTokenType(Claims claims) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);

        if (tokenType == null) {
            throw new IllegalArgumentException("토큰 타입이 없습니다.");
        }

        return TokenType.valueOf(tokenType);
    }

    private MemberRole getRole(Claims claims) {
        String role = claims.get(ROLE_CLAIM, String.class);

        if (role == null) {
            throw new IllegalArgumentException("권한 정보가 없습니다.");
        }

        return MemberRole.valueOf(role);
    }
}