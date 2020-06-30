package org.wso2.scim.metadata.population.tool.util;

import org.wso2.scim.metadata.population.tool.IdentityException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigParser {

    private static final String DEFAULT_CONFIG_FILE = "config.properties";
    private final Properties properties = new Properties();

    public ConfigParser() throws IdentityException {
        this(DEFAULT_CONFIG_FILE);
    }
    public ConfigParser(String configFile) throws IdentityException {
        try (InputStream input = new FileInputStream(configFile)) {
            properties.load(input);
        } catch (IOException e) {
            throw new IdentityException("Can't find/read config file: " + configFile, e);
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }
}
