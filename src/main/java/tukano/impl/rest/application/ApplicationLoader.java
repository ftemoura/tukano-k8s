package tukano.impl.rest.application;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.core.Application;
import tukano.impl.Token;
import utils.ConfigLoader;

public class ApplicationLoader extends Application
{

    private Application instance;

    public ApplicationLoader() {
        try {
            Class<?> clazz = Class.forName(ConfigLoader.getInstance().getApplicationClass());
            if (Application.class.isAssignableFrom(clazz)) {
                this.instance = (Application) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<Class<?>> getClasses() {
        return this.instance.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return this.instance.getSingletons();
    }
}


