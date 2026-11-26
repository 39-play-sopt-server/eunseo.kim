package org.sopt.blog.domain.auth.client;

import org.sopt.blog.domain.auth.entity.OAuthProvider;

import java.net.URI;

public interface OAuthClient {

    OAuthProvider provider();

    URI createAuthorizationUri(String state);

    OAuthUser getOAuthUser(String authorizationCode);
}
