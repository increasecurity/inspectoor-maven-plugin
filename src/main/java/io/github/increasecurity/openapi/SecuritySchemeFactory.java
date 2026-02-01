package io.github.increasecurity.openapi;

import io.github.increasecurity.model.security.*;

import java.util.HashMap;
import java.util.Map;

public class SecuritySchemeFactory {
    public static SecurityScheme create(Map<String, Object> data) {
        String type = (String) data.get("type");

        switch (type) {
            case "apiKey":
                ApiKeySecurityScheme apiKey = new ApiKeySecurityScheme();
                apiKey.name = (String) data.get("name");
                apiKey.in = (String) data.get("in");
                apiKey.type = type;
                apiKey.description = (String) data.get("description");
                return apiKey;

            case "http":
                HttpSecurityScheme http = new HttpSecurityScheme();
                http.scheme = (String) data.get("scheme");
                http.bearerFormat = (String) data.get("bearerFormat");
                http.type = type;
                http.description = (String) data.get("description");
                return http;

            case "oauth2":
                OAuth2SecurityScheme oauth = new OAuth2SecurityScheme();
                oauth.type = type;
                oauth.description = (String) data.get("description");
                Map<String, Map<String, Object>> flows = (Map<String, Map<String, Object>>) data.get("flows");
                Map<String, OAuth2SecurityScheme.OAuthFlow> flowObjs = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> flow : flows.entrySet()) {
                    OAuth2SecurityScheme.OAuthFlow flowObj = new OAuth2SecurityScheme.OAuthFlow();
                    flowObj.authorizationUrl = (String) flow.getValue().get("authorizationUrl");
                    flowObj.tokenUrl = (String) flow.getValue().get("tokenUrl");
                    flowObj.refreshUrl = (String) flow.getValue().get("refreshUrl");
                    flowObj.scopes = (Map<String, String>) flow.getValue().get("scopes");
                    flowObjs.put(flow.getKey(), flowObj);
                }
                oauth.flows = flowObjs;
                return oauth;

            case "openIdConnect":
                OpenIdConnectSecurityScheme openid = new OpenIdConnectSecurityScheme();
                openid.type = type;
                openid.description = (String) data.get("description");
                openid.openIdConnectUrl = (String) data.get("openIdConnectUrl");
                return openid;

            default:
                throw new IllegalArgumentException("Unknown security scheme type: " + type);
        }
    }
}

