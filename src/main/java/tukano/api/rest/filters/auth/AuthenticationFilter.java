package tukano.api.rest.filters.auth;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import tukano.impl.Token;

import java.security.Principal;


@AuthRequired
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter
{
    private static final String AUTHENTICATION_SCHEME = "Bearer";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            String token = requestContext.getCookies().get(Token.Service.AUTH.toString()).getValue();
            if (token == null) { //!authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ")) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
            }
            if (!Token.isValid(token, Token.Service.AUTH))
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());

            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new SecurityContext() {

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
                    return currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return AUTHENTICATION_SCHEME;
                }
            });

        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}
