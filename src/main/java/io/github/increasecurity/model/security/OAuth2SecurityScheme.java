package io.github.increasecurity.model.security;

import java.util.Map;

public class OAuth2SecurityScheme extends SecurityScheme {
    public Map<String, OAuthFlow> flows;

    public static class OAuthFlow {
        public String authorizationUrl;
        public String tokenUrl;
        public String refreshUrl;
        public Map<String, String> scopes;
    }
}
