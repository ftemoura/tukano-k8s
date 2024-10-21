package tukano.impl.rest;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.JavaUsers;

public class MainApplication extends Application
{
    private Set<Object> singletons = new HashSet<>();
    private Set<Class<?>> resources = new HashSet<>();

    public MainApplication() {
        singletons.add(JavaShorts.class);
        singletons.add(JavaBlobs.class);
        singletons.add(JavaUsers.class);
        resources.add(RestBlobsResource.class);
        resources.add(RestShortsResource.class);
        resources.add(RestUsersResource.class);
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


