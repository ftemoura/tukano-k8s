package utils;

import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

public class FakeSecurityContext {
    public static SecurityContext get(String userId) {
        return new SecurityContext() {

            @Override
            public Principal getUserPrincipal() {
                return () -> userId;
            }

            @Override
            public boolean isUserInRole(String role) {
                return true;
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
