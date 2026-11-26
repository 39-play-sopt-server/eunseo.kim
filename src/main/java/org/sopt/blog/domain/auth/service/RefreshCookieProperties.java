package org.sopt.blog.domain.auth.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt.refresh-cookie")
public class RefreshCookieProperties {

    private String name;
    private String path;
    private boolean secure;
    private String sameSite;
}
