package com.veingraph.auth.oauth2;

import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;

import java.util.Collections;
import java.util.Map;

/**
 * Resource Owner Password Credentials Grant 认证 Token
 */
public class ResourceOwnerPasswordAuthenticationToken extends OAuth2AuthorizationGrantAuthenticationToken {

    private final String username;
    private final String password;
    private final String clientId;

    public ResourceOwnerPasswordAuthenticationToken(
            AuthorizationGrantType authorizationGrantType,
            Authentication clientPrincipal,
            @Nullable Map<String, Object> additionalParameters) {
        super(authorizationGrantType, clientPrincipal, additionalParameters);
        this.username = additionalParameters != null ? (String) additionalParameters.get("username") : null;
        this.password = additionalParameters != null ? (String) additionalParameters.get("password") : null;
        this.clientId = additionalParameters != null ? (String) additionalParameters.get("client_id") : null;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public Map<String, Object> getAdditionalParameters() {
        return Collections.unmodifiableMap(super.getAdditionalParameters());
    }
}