package org.sopt.blog.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "admin.bootstrap")
public class AdminBootstrapProperties {

    private String email;
}
