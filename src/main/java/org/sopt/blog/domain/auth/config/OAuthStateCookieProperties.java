package org.sopt.blog.domain.auth.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "oauth.state-cookie")
public class OAuthStateCookieProperties {

    @NotBlank
    private String name;

    @NotBlank
    private String path;

    private boolean secure;

    @NotBlank
    private String sameSite;

    @Positive
    private long maxAgeSeconds;
}
