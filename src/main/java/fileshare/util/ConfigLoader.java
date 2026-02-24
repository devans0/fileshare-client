/**
 * title: Configuration Loader
 * description: Utility for parsing and returning values from ".properties" files
 * @author Dominic Evans
 * @date January 22 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans 
 */

package fileshare.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import fileshare.client.FSClientGUI;

public class ConfigLoader {
	private static final Properties props = new Properties();
	
	/*
	 * Initialize the ConfigLoader
	 * 
	 * This static initializer first tries to locate a properties file in the
	 * current directory before falling back on an internal resource (when compiled
	 * as a .jar). This allows the user to override configuration by changing the
	 * client.properties file which resides in the root directory but the file
	 * remains optional.
	 */
	static {
		// Try to get the external file
		File external = new File("client.properties");
		
		try {
			if (external.exists()) {
				try (InputStream in = new FileInputStream(external)) {
					props.load(in);
					System.out.println("[CONFIG] Loaded external client.properties");
				}
			} else {
				// Fallback to the internal file
				try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("client.properties")) {
					if (in != null) {
						props.load(in);
						System.out.println("[CONFIG] Loaded internal default client.properties");
					} else {
						throw new IOException("Internal client.properties could not load.");
					}
				}
			}
		} catch (IOException ioe) {
			String errmsg = "[CONFIG] Critical error loading properties: " + ioe.getMessage();
			ioe.printStackTrace();
			FSClientGUI.showErrorPopup("Configuration Error", errmsg, false);
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
		// Handle the case where there is no set share directory
		if (newVal == null) {
			props.remove(key);
		} else {
			props.put(key, newVal);
		}

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
