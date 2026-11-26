package org.sopt.blog.domain.auth.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "kakao")
public class KakaoProperties {

    @NotBlank
    private String restApiKey;

    @NotBlank
    private String clientSecret;

    @NotBlank
    private String redirectUri;
}
