package org.sopt.blog;

import org.sopt.blog.domain.auth.service.RefreshCookieProperties;
import org.sopt.blog.global.config.AdminBootstrapProperties;
import org.sopt.blog.global.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.sopt.blog.domain.auth.config.KakaoProperties;
import org.sopt.blog.domain.auth.config.OAuthStateCookieProperties;
import org.sopt.blog.domain.auth.config.OAuthProperties;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        RefreshCookieProperties.class,
        AdminBootstrapProperties.class,
        KakaoProperties.class,
        OAuthStateCookieProperties.class,
        OAuthProperties.class
})
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
