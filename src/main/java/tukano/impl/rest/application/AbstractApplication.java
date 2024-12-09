package tukano.impl.rest.application;

import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import tukano.impl.Token;
import utils.ConfigLoader;

public abstract class AbstractApplication extends Application
{
    protected Set<Object> singletons = new HashSet<>();
    protected Set<Class<?>> resources = new HashSet<>();

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s");
    }

    public AbstractApplication() {
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


