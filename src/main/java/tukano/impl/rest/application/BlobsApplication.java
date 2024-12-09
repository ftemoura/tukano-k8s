package tukano.impl.rest.application;

import tukano.api.rest.filters.auth.AuthenticationFilter;
import tukano.impl.JavaBlobs;
import tukano.impl.rest.RestBlobsResource;

public class BlobsApplication extends AbstractApplication {

    public BlobsApplication() {
        singletons.add(JavaBlobs.class);
        resources.add(RestBlobsResource.class);
        resources.add(AuthenticationFilter.class);
    }
}
