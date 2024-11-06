package tukano.impl.rest;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import tukano.api.rest.filters.auth.AuthenticationFilter;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.JavaUsers;
import tukano.impl.Token;
import utils.ConfigLoader;

public class MainApplication extends Application
{
    private Set<Object> singletons = new HashSet<>();
    private Set<Class<?>> resources = new HashSet<>();

    static final String INETADDR_ANY = "0.0.0.0";
    static String FUNCTIONS_BASE_URI = ConfigLoader.getInstance().getFunctionsURL();

    public static final int PORT = 8080;

    public static String serverURI;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
    }

    public MainApplication() {
        singletons.add(JavaShorts.class);
        singletons.add(JavaBlobs.class);
        singletons.add(JavaUsers.class);
        resources.add(RestBlobsResource.class);
        resources.add(RestShortsResource.class);
        resources.add(RestUsersResource.class);
        resources.add(AuthenticationFilter.class);
        serverURI = String.format(FUNCTIONS_BASE_URI, PORT);
        Token.setSecret(ConfigLoader.getInstance().getTokenSecret());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}


