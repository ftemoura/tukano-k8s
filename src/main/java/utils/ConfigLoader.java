package utils;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static ConfigLoader instance;
    private Properties properties = new Properties();

    private ConfigLoader() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                System.out.println("Sorry, unable to find application.properties");
                return;
            }
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to fakeSecurityContext the singleton instance
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public String getUsedDbType() {
        return System.getenv("USED_DB_TYPE");
    }

    public String getTokenSecret() {
        return System.getenv("TOKEN_SECRET");
    }

    public String getRegion() {
        return System.getenv("AZURE_REGION");
    }

    public String getRedisHostname() {
        return System.getenv("REDIS_URL");
    }

    public String getRedisKey() {
        return System.getenv("REDIS_KEY");
    }

    public int getRedisPort() {
        return Integer.parseInt(System.getenv("REDIS_PORT"));
    }

    public String getHibernateUsername() {return System.getenv("POSTGRES_USER");}

    public String getHibernatePassword() {return System.getenv("POSTGRES_PASSWORD");}

    public String getHibernateConnectionUrl() {
        return System.getenv("POSTGRES_JDBC_URL");
    }

    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(System.getenv("CACHE_ENABLED"));
    }

    public String getApplicationClass() {
        return System.getenv("APPLICATION_CLASS");
    }

    public String getShortsInternalEndpoint() {
        return System.getenv("SHORTS_INTERNAL_ENDPOINT");
    }

    public String getUsersInternalEndpoint() {
        return System.getenv("USERS_INTERNAL_ENDPOINT");
    }

    public String getBlobsInternalEndpoint() {
        return System.getenv("BLOBS_INTERNAL_ENDPOINT");
    }

    public String getExternalEndpoint() {
        return System.getenv("EXTERNAL_ENDPOINT");
    }

    public String getHibernateConfigPath() {
        return properties.getProperty("hibernate.config");
    }
}