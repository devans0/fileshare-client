/**
 * title: Share Manager
 * description: Tracks, lists and manages the sharing of local files 
 * @author Dominic Evans
 * @date January 29, 2026, February 16, 2026
 * @version 2.0
 * @copyright 2026 Dominic Evans
 */

package fileshare.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fileshare.generated.FileShareWS;

/*
 * The ShareManager is responsible for managing the local share directory and the files that it contains.
 * Its methods are used to both perform initial registration of files in the share directory as well as 
 * periodically scanning that directory for changes, either new files being added or files being removed.
 * Any changes to the share directory are reflected through updates to the share server.
 */

public class ShareManager {
	
	private final Path shareDir;
	private final FileShareWS server;
	private final String peerID;
	private final String localAddress;
	private final int localPort;
	private final Set<Path> registeredFiles = new HashSet<>();
	private final Set<Path> excludedFiles = new HashSet<>();
	
	public ShareManager(Path shareDir, FileShareWS tracker, String peerID, String localAddress, int localPort) {
		this.shareDir = shareDir;
		this.server = tracker;
		this.peerID = peerID;
		this.localAddress = localAddress;
		this.localPort = localPort;
	} // ctor

	/**
	 * Creates a new scheduled thread to periodically scan the share directory for changes.
	 * 
	 * @param int The number of seconds between heartbeats and updates of the share directory.
	 */
	public void startMonitoring(int period) {
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::syncDirectory, 0, period, TimeUnit.SECONDS);
	}
	
	/**
	 * Adds a file path to the set of registered files. This file will be shared
	 * with other consumers connected to the Web service and becomes available for
	 * download. If the path is currently block-listed, this will remove it from the
	 * excludedFiles set.
	 * 
	 * @param filePath the path of the file to be included for sharing.
	 */
	public void registerFile(Path filePath) {
		if (excludedFiles.contains(filePath)) {
			excludedFiles.remove(filePath);
		}
		registeredFiles.add(filePath);
	}

	/**
	 * Indicates that a file should not be shared by block-listing its file path.
	 * Block listing is required to allow the user to stop sharing files that are in
	 * a configured share directory if they wish.
	 * 
	 * @param filePath the path of the file that is not to be shared.
	 */
	public void excludeFile(Path filePath) {
		if (registeredFiles.contains(filePath)) {
			registeredFiles.remove(filePath);
		}
		excludedFiles.add(filePath);
	}
	
	/**
	 * Scans each of the files in the shared directory and processes them so long as they
	 * are regular files and they are not hidden. Additionally responsible for sending a 
	 * heartbeat signal to the server to indicate that the client is still present and providing
	 * files.
	 */
	private void syncDirectory() {
		// Track the number of attempted retries and the need for another one
		int retries = 0;
		boolean needsRetry = false;
		
		/*
		 * This loop enables immediate retries of the synchronization. This may be
		 * necessary if the server does not have any records of the files that the
		 * client expects to have registered with the server.
		 * 
		 * The call to keepAlive() that constitutes the heartbeat may return false which
		 * indicates that the server has not updated any listings that are associated
		 * with the client. The loop exits if there are no files currently being tracked
		 * so the client can safely assume that it is sharing at least one file at the
		 * time a heartbeat is sent. Thus, a false return on the heartbeat indicates
		 * that there is some issue; either active files have been reaped by the server
		 * or listing of the files failed. In either case, the client should immediately
		 * attempt to re-register its entire share directory.
		 * 
		 * The number of retries are limited to three per invocation of this method to
		 * avoid having a client overwhelm the server with retries in the event of a
		 * malfunction.
		 */
		do {
			// Reset
			needsRetry = false;
			try {
				// Get the files that are current present in the share directory
				Set<Path> currDiskFiles = new HashSet<>();
				try (Stream<Path> stream = Files.list(this.shareDir)) {
					List<Path> paths = stream.collect(Collectors.toList());

					// No files in the directory; immediately exit the loop/method
					if (paths.isEmpty()) { 
						break; 
					}

					paths.stream().filter(Files::isRegularFile).filter(p -> {
						try {
							return !Files.isHidden(p);
						} catch (IOException ioe) {
							return false;
						}
					}).forEach(p -> currDiskFiles.add(p));
				}

				// Identify any new files; register them with the share server
				for (Path filePath : currDiskFiles) {
					if (!registeredFiles.contains(filePath)) {
						try {
							String fileName = filePath.getFileName().toString();
							server.listFile(this.peerID, fileName, this.localAddress, this.localPort);
							registeredFiles.add(filePath);
							System.out.println("[SHARE] Registered: " + filePath);
						} catch (Exception e) {
							System.err.println("[SHARE] Registration failed: " + filePath);
						}
					}
				}

				/*
				 * Identify any files that have been removed from the directory
				 * If any exist, delist them from the share server
				 */
				Iterator<Path> it = registeredFiles.iterator();
				while (it.hasNext()) {
					Path knownFile = it.next();
					if (!currDiskFiles.contains(knownFile)) {
						try {
							String knownFileName = knownFile.getFileName().toString();
							server.delistFile(knownFileName, this.peerID);
							it.remove();
							System.out.println("[SHARE] Delisted: " + knownFile);
						} catch (Exception e) {
							System.err.println("[SHARE] Delisting failed: " + knownFile);
						}
					}
				}
				
				/* Send the heartbeat and detect if the server has lost the file listings.
				   If the server has reaped the listed files between heartbeats due to a delay, then
				   this check will cause an immediate refresh of the share directory */
				if (!server.keepAlive(this.peerID)) {
					System.out.println("[SHARE] Session lost. Attempting immediate re-sync...");
					registeredFiles.clear();
					// Limit the number of retries to 3
					needsRetry = (retries++ < 3);
				}

			} catch (IOException ioe) {
				System.err.println("[SHARE] Directory sync error " + ioe.getMessage());
				ioe.printStackTrace();
			} 
		} while (needsRetry);
	} // syncDirectory
	
	/**
	 * Wrapper method for disconnecting from the server. Ensures that the ShareManager provides the total
	 * interface for all functions related to sharing files.
	 * 
	 * Disconnecting involves alerting the server to delist all files that are associated with this client.
	 */
	public void disconnect() {
		server.disconnect(this.peerID);
	}
}




















