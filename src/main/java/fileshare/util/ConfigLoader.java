/**
 * title: Configuration Loader
 * description: Utility for parsing and returning values from ".properties" files
 * @author Dominic Evans
 * @date January 22 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans 
 */

package main.java.fileshare.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class ConfigLoader {
	private static Properties props = new Properties();
	
	/* 
	 * Initialize the ConfigLoader
	 * 
	 * getResourceAsStream necessary in order to properly integrate with Wildfly.
	 * Otherwise, the configuration file will be searched for in the working directory
	 * of the execution which will be the Wildfly home directory. This ensures
	 * that the configuration file can be found.
	 * 
	 * This requires that the configuration file be found in src/main/resources so
	 * that it is properly placed where Wildfly may find it.
	 */
	static {
		File configFile = new File("client.properties");
		if (configFile.exists()) {
			try (InputStream is = new FileInputStream(configFile)) {
				props.load(is);
			} catch (IOException ioe) {
				System.err.println("[ERROR] client.properties not found at " + configFile.getAbsolutePath());
				ioe.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns the value associated with the provided key
	 * @param key indicating which property to access
	 * @return String value associated with the desired property
	 */
	public static String getProperty(String key) {
		return props.getProperty(key);
	} // getProperty
	
	/* Overload: provide an optional default configuration value */
	public static String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	} // getProperty
	
	/**
	 * Saves a new or updated property to the properties file.
	 * 
	 * @param key the key of the new/updated property.
	 * @param newVal the value of the new/updated property.
	 */
	public static void saveProperty(String key, String newVal) {
		props.put(key, newVal);
		try (OutputStream os = new FileOutputStream("client.properties")) {
			props.store(os, "Updated by FSClient");
		} catch (IOException ioe) {
			System.err.println("[ERROR] ConfigLoader: failed to write properties to disk.");
		}
	} //saveProperty
	
	/**
	 * Fetches the integer value associated with a given key
	 * @param key indicating which property to access; must be associated with an
	 * integer-valued property field
	 * @return Integer value associated with the provided key
	 */
	public static int getIntProperty(String key) {
		String resultStr = props.getProperty(key);
		if (resultStr == null) {
			throw new RuntimeException("Missing required configuration key: " + key);
		}
		return Integer.parseInt(resultStr);
	} // getIntProperty
	
	/* Overload: provide an optional default configuration value */
	public static int getIntProperty(String key, int defaultVal) {
		String resultStr = props.getProperty(key);
		if (resultStr == null) {
			return defaultVal;
		} 
		return Integer.parseInt(resultStr);
	} // getIntProperty
	
	/**
	 * Fetches the boolean value associated with a given key and returns its value
	 * @param key indicating the property to be returned from the configuration file.
	 * @return boolean corresponding to desired key
	 */
	public static boolean getBooleanProperty(String key) {
		return Boolean.parseBoolean(props.getProperty(key));
	} // getBooleanProperty  
	
	/* Overload: provide an optional default configuration value */
	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		String val = props.getProperty(key);
		if (val == null) {
			return defaultValue;
		}
		return Boolean.parseBoolean(val);
	} // getBooleanProperty
}
