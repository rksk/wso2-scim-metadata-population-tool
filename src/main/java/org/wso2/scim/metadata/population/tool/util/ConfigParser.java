package org.wso2.scim.metadata.population.tool.util;

import org.wso2.scim.metadata.population.tool.IdentityException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigParser {

    private static final String CONFIG_FILE = "config.properties";
    private final Properties properties = new Properties();

    public ConfigParser() throws IdentityException {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IdentityException("Can't find/read config file: " + CONFIG_FILE, e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
