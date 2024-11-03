package utils;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private static ConfigLoader instance;
    private Properties properties = new Properties();
    private Properties secrets = new Properties();

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
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("application.secrets")) {
            if (inputStream == null) {
                System.out.println("Sorry, unable to find se.properties");
                return;
            }
            secrets.load(inputStream);
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

    public String getRegion() {
        return secrets.getProperty("AZURE_REGION");
    }

    public String getBlobConnectionString() {
        return secrets.getProperty("AZURE_BLOB_STORE_CONNECTION");
    }

    public String getRedisHostname() {
        return secrets.getProperty("REDIS_URL");
    }

    public String getRedisKey() {
        return secrets.getProperty("REDIS_KEY");
    }

    public int getRedisPort() {
        return Integer.parseInt(secrets.getProperty("REDIS_PORT"));
    }

    public String getCosmosDBKey() {
        return secrets.getProperty("AZURE_COSMOSDB_KEY");
    }

    public String getCosmosDBName() {
        return secrets.getProperty("AZURE_COSMOSDB_NAME");
    }

    public String getCosmosConnectionUrl() {
        return secrets.getProperty("AZURE_COSMOSDB_ENDPOINT");
    }

    public String getHibernateUsername() {return secrets.getProperty("HIBERNATE_USERNAME");}

    public String getHibernatePassword() {return secrets.getProperty("HIBERNATE_PWD");}

    public String getHibernateConnectionUrl() {
        return secrets.getProperty("HIBERNATE_JDBC_URL");
    }

    public String getHibernateConfigPath() {
        return properties.getProperty("hibernate.config");
    }

    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(properties.getProperty("cacheEnabled"));
    }

    public String getblobShortsName() {
        return properties.getProperty("azure.blobShortsName");
    }

    public String getCosmosDBUsersContainer() {
        return properties.getProperty("azure.cosmosDBUsersContainer");
    }

    public String getCosmosDBShortsContainer() {
        return properties.getProperty("azure.cosmosDBShortsContainer");
    }

    public String getCosmosDBLikesContainer() {
        return properties.getProperty("azure.cosmosDBLikesContainer");
    }

    public String getCosmosDBFollowsContainer() {
        return properties.getProperty("azure.cosmosDBFollowsContainer");
    }
}