/**
 * title: Identity Manager
 * description: Provides unique identity for a client that persists over client instances
 * @author Dominic Evans
 * @date January 25, 2026; February 17, 2026
 * @version 2.0
 * @copyright 2026 Dominic Evans
 */

/**
 * Class IdentityManager is responsible for providing a UUID for a client that is used for identifying
 * a client with a remote server. This UUID shall provide a means of distinguishing any two clients in
 * the case of other distinguishing features being insufficient. 
 * 
 * In order to provide some persistence, this class maintains an identity file that stores the generated 
 * UIUD for additional uses. This identity file persists so long as it has been accessed within the last
 * 24 hours. Otherwise, the identity file is replaced with a new one containing a fresh UUID. The idea
 * is to minimally resist impersonation attempts while allowing for the eventuality of a lost connection.
 * 
 * 2.0 Changes
 * IdentityManager has been updated to provide a static interface. The loadOrCreateID has been replaced
 * by getUUID. The UUID is no longer stored in a member, but is stored in the identity file and then
 * returned to the caller of the method as a string. The constructor has been made private to facilitate
 * guarding the class from object instantiation.
 */

package main.java.fileshare.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class IdentityManager {
	
	// Guard: this class is to be used statically
	private IdentityManager() {}
	
	/**
	 * Creates a new peerID or loads it from the disk if there is a cached peerID available
	 * 
	 * @return String UUID that was either generated or loaded from disk
	 */
	public static String getUUID() {
		// Retrieve the client identity file name from configuration
		String identityFileName = ConfigLoader.getProperty("client.identity_file", ".client_id");
		Path idPath = Paths.get(identityFileName);

		try {
			// Create the identity file if it does not already exist and ensure that it has
			// the proper attributes to be hidden on any platform
			if (!Files.exists(idPath)) {
				Files.createFile(idPath);

				// Set windows file property if necessary (i.e. platform is Windows)
				String os = System.getProperty("os.name").toLowerCase();
				if (os.contains("win")) {
					Files.setAttribute(idPath, "dos:hidden", true);
				}
			}

			// If the file is expired, generate and store a new UUID before returning it;
			// otherwise, the stored UUID is sufficient and will not be touched
			if (isIDExpired(idPath) || Files.size(idPath) == 0) {
				String uuid = UUID.randomUUID().toString();
				Files.write(idPath, uuid.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE,
						StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
				return uuid;
			}
			// The existed and was not expired; return the UUID stored there
			return new String(Files.readAllBytes(idPath), StandardCharsets.UTF_8).trim();
		} catch (IOException ioe) {
			System.err.println("[SYS] Could not set hidden attribute for identity file: " + ioe.getMessage());
			// Fallback: if there is some IOExcpetion, use a random UUID to allow the client
			// to function
			return UUID.randomUUID().toString();
		}
	} // loadOrCreateID

	/**
	 * Determines if the last access of a file occurred more than 24 hours ago.
	 * 
	 * @param idFilePath the Path of the file
	 * @return true if the last access of the file was 24 hours or more in the past
	 *         false otherwise
	 */
	private static boolean isIDExpired(Path idFilePath) {
		try {
			// Get file attributes and calculate file age based on last access time
			BasicFileAttributes idFileAttrs = Files.readAttributes(idFilePath, BasicFileAttributes.class);
			long lastAccessTime = idFileAttrs.lastAccessTime().toMillis();
			long fileAge = System.currentTimeMillis() - lastAccessTime;
			long expiry = TimeUnit.HOURS.toMillis(24);

			// Determine if file is expired
			if (fileAge > expiry) {
				return true;
			} else {
				return false;
			}
		} catch (IOException ioe) {
			System.err.println("[ID] Error checking file age: " + ioe.getMessage());
		}
		// Should be unreachable but default to assuming the file is expired
		return true;
	} // isIDExpired
}












