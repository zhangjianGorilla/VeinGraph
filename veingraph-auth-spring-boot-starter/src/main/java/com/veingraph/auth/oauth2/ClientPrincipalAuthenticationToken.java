package com.veingraph.auth.oauth2;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * OAuth2 客户端认证 Token
 */
public class ClientPrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final String clientId;
    private final String clientSecret;

    public ClientPrincipalAuthenticationToken(String clientId, String clientSecret, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return clientSecret;
    }

    @Override
    public Object getPrincipal() {
        return clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }
}