package org.sopt.blog.global.config;

import org.sopt.blog.global.security.JwtAuthenticationFilter;
import org.sopt.blog.global.security.JwtProvider;
import org.sopt.blog.domain.auth.repository.RevokedAccessTokenRepository;
import org.sopt.blog.domain.member.repository.MemberRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final MemberRepository memberRepository;

    public SecurityConfig(
            JwtProvider jwtProvider,
            RevokedAccessTokenRepository revokedAccessTokenRepository,
            MemberRepository memberRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.memberRepository = memberRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                // CSRF 비활성화, Authorization 헤더로 보내면 CSRF의 전제 조건이 없기 때문
                .csrf(AbstractHttpConfigurer::disable)
                // 폼 로그인, HTTP Basic 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 세션 STATELESS
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/signup", "/auth/login", "/auth/reissue").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/*", "/auth/*/callback").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/members").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // 인증 실패 시 401 응답
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpStatus.FORBIDDEN.value())))
                // JWT 인증 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                jwtProvider,
                                revokedAccessTokenRepository,
                                memberRepository
                        ),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
