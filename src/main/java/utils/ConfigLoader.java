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

    // Method to get the singleton instance
    public static synchronized ConfigLoader getInstance() {
        if (instance == null) {
            instance = new ConfigLoader();
        }
        return instance;
    }

    public String getBlobConnectionString() {
        return properties.getProperty("azure.blobConnectionString");
    }

    public String getblobShortsName() {
        return properties.getProperty("azure.blobShortsName");
    }

    public String getRedisHostname() {
        return properties.getProperty("azure.redisHostname");
    }

    public String getRedisKey() {
        return properties.getProperty("azure.redisKey");
    }

    public String getHibernateConfigPath() {
        return properties.getProperty("hibernateConfig");
    }

    public int getRedisPort() {
        return Integer.parseInt(properties.getProperty("azure.redisPort"));
    }

    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(properties.getProperty("azure.cacheEnabled"));
    }

    public String getCosmosConnectionUrl() {
        return properties.getProperty("azure.cosmosConnectionUrl");
    }

    public String getCosmosDBKey() {
        return properties.getProperty("azure.cosmosDBKey");
    }

    public String getCosmosDBName() {
        return properties.getProperty("azure.cosmosDBName");
    }

    public String getCosmosDBUsersContainer() {
        return properties.getProperty("azure.cosmosDBUsersContainer");
    }

    public String getCosmosDBShortsContainer() {
        return properties.getProperty("azure.cosmosDBShortsContainer");
    }
}