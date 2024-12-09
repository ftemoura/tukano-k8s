package utils;

import jakarta.ws.rs.core.SecurityContext;
import tukano.impl.Token;

import java.security.Principal;

public class Auth {
    public static SecurityContext fakeSecurityContext(String token) {
        return new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return () -> token;
            }

            @Override
            public boolean isUserInRole(String role) {
                return Token.isEnoughRoleLevel(token, Token.Service.AUTH, Token.Role.valueOf(role));
            }

            @Override
            public boolean isSecure() {
                return true;
            }

            @Override
            public String getAuthenticationScheme() {
                return "";
            }
        };
    }
}
