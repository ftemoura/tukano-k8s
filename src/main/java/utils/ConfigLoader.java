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
}