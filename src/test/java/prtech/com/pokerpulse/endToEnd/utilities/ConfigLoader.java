package prtech.com.pokerpulse.endToEnd.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final Properties PROPERTIES = loadProperties();

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                System.out.println(input);
                props.load(input);
            }
        } catch (IOException e) {
            System.out.println("Error loading config.properties file");
        }
        return props;
    }

    public static String getProperty(String key) {
        String systemProp = System.getProperty(key);
        return systemProp != null ? systemProp : PROPERTIES.getProperty(key);
    }
    public static void getAllProperties(){
        for (Object key: PROPERTIES.keySet()){
            System.out.println(PROPERTIES.get(key));
            System.out.println(PROPERTIES.getProperty(key.toString()));
        }
    }

    public static void main(String[] args) {
        ConfigLoader.getAllProperties();
    }
}
