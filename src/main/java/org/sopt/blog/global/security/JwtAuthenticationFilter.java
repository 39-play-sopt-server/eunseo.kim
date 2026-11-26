package org.sopt.blog.global.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final MemberRepository memberRepository;

    private final JwtProvider jwtProvider;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            RevokedAccessTokenRepository revokedAccessTokenRepository,
            MemberRepository memberRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (token != null) {
            try {
                TokenPayload payload = jwtProvider.parseAccessToken(token);
                if (revokedAccessTokenRepository.existsByJti(payload.jti())
                        || !memberRepository.existsByIdAndDeletedAtIsNull(payload.memberId())) {
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                payload.memberId(),
                                null,
                                List.of(new SimpleGrantedAuthority(payload.role().name()))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 유효하지 않은 토큰이면 인증 정보를 저장하지 않음
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
