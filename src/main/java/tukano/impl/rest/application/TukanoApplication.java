package tukano.impl.rest.application;

import tukano.api.rest.filters.auth.AuthenticationFilter;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.JavaUsers;
import tukano.impl.rest.RestBlobsResource;
import tukano.impl.rest.RestShortsResource;
import tukano.impl.rest.RestUsersResource;
import utils.ConfigLoader;

import java.util.HashSet;
import java.util.Set;

public class TukanoApplication extends AbstractApplication
{
    public TukanoApplication() {
        singletons.add(JavaShorts.class);
        singletons.add(JavaBlobs.class);
        singletons.add(JavaUsers.class);
        resources.add(RestBlobsResource.class);
        resources.add(RestShortsResource.class);
        resources.add(RestUsersResource.class);
        resources.add(AuthenticationFilter.class);
    }
}


