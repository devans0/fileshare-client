/**
 * title: Share Manager
 * description: Tracks, lists and manages the sharing of local files 
 * @author Dominic Evans
 * @date January 29, 2026, February 16, 2026
 * @version 2.0
 * @copyright 2026 Dominic Evans
 */

package fileshare.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import fileshare.util.ShareUpdateListener;
import jakarta.xml.ws.WebServiceException;

/**
 * ShareManager is responsible for managing the set of files that the user wishes to share with peers. These
 * files may be sourced from a configured share directory which may or may not exist or they can be manually
 * indicated by a user as files for sharing. If a share directory is configured, then ShareManager will
 * periodically scan it in order to detect any changes in the set of files residing there and update the remote
 * share server accordingly. ShareManager is thread safe and prepared for a multi-threaded environment in which
 * a user may make state changes simultaneously with scheduled share maintenance. 
 * 
 * 2.0 Changes
 * The second version of this class adds the capacity for managing shares from more than one source, namely that
 * of files that are manually indicated by the user to be either shared or not shared. Excluded files will be
 * ignored during share directory scans, if they are present there. Shared files are tracked so that they may
 * be shared even in the case where they do not reside in the shared directory. Thus, the listFile() and
 * excludFile() methods are new. 
 * 
 * The class now additionally supports configuring the share directory to a new one at any time. This is supported
 * through the setShareDir() method. The current directory used for sharing can be retrieved via getShareDir().
 * 
 * The multiple sources of shared files as well as the multiple sources of updates to the set of shared files
 * demanded that the syncFiles() method be made thread safe. This is accomplished by utilizing a syncLock object
 * to lock the syncFiles() method when a thread is executing it. Additionally, if another thread changes the 
 * shared directory location while the syncFiles() method is in progress, the update must be immediately applied
 * to avoid corrupting the state of shared files. This is accomplished by setting a dirty bit when the shared
 * directory changes, via the changePending volatile member. If an update completes and detects that this bit is
 * set, the thread that completed the update will immediately re-update the shared files. This assures consistent
 * state and that all intended files are shared while no unintended files remain shared.
 */

public class ShareManager {

	private Path shareDir;
	private final String peerID;
	private final String localAddress;
	private final int localPort;
	private ScheduledExecutorService scheduler;
	private final Map<String, Path> listedFiles = new ConcurrentHashMap<>();     // Files currently registered with the server
	private final Set<Path> userSharedFiles = new HashSet<>(); 					 // Files that the user wishes to share
	private final Set<Path> excludedFiles = new HashSet<>();   				     // Files that the user indicates not to share
	private final List<ShareUpdateListener> listeners = new ArrayList<>();

	// Concurrency controls
	private volatile boolean changePending = false;
	private final ReentrantLock syncLock = new ReentrantLock();

	public ShareManager(String peerID, String localAddress, Path shareDir, int localPort) {
		this.peerID = peerID;
		this.localAddress = localAddress;
		this.shareDir = shareDir;
		this.localPort = localPort;
	} // ctor

	// shareDir getter
	public Path getShareDir() {
		return this.shareDir;
	}

	/**
	 * Set the share directory. If there are any files in the exclude files set that
	 * are in a newly-set shared directory then those files will be removed from the
	 * excluded set. This avoids the situation in which a user excludes a file and
	 * then chooses to share the directory it resides in. Having only some of the
	 * files appear in the newly-shared directory will appear to be an error and
	 * will also require the user to manually re-add any previously excluded files.
	 * 
	 * @param newShareDir the Path to the new shared directory; may be null to stop
	 *                 sharing any directories.
	 */
	public void setShareDir(Path newShareDir) {
		// De-list all files in the current share directory
		if (this.shareDir != null && Files.exists(this.shareDir)) {
			try (Stream<Path> stream = Files.list(this.shareDir)){
				
				stream.filter(Files::isRegularFile)
					  .forEach(p -> {
						  userSharedFiles.remove(p);
						  excludedFiles.remove(p);
						  delistFile(p);
					  });
			} catch (IOException ioe) {
				System.err.println("[SHARE] Error de-listing directory: " + ioe.getMessage());
			}
		}
		
		// Set the new directory; may be null indicating that the directory was unset
		this.shareDir = newShareDir;

		// Remove files in the new directory from the excluded list if it exists
		if(this.shareDir != null && Files.exists(this.shareDir)) {
			try (Stream<Path> stream = Files.list(this.shareDir)) {
				stream.filter(Files::isRegularFile).filter(p->{
					try {
						return !Files.isHidden(p);
					} catch (IOException ioe) {
						return false;
					}
				}).forEach(excludedFiles::remove);
			} catch (IOException ioe) {
				System.err.println("[SHARE] set share directory error: " + ioe.getMessage());
			}
		}
		// Sync files immediately if shareDir changes
		// Indicate that there is a change pending in case another thread has syncFiles locked
		this.changePending = true;
		new Thread(() -> {
			String msg = (this.shareDir == null) ? "Unset share directory." : "New directory: " + this.shareDir;
			System.out.println("[SHARE] " + msg + " Synchronizing shared files...");
			syncFiles();
		}).start();
	} // setShareDir
	
	/**
	 * Search the hash map to find a file that is being shared according to its name.
	 * 
	 * @param fileName the name of the desired file
	 * @return Path of the file if it exists in the map; null otherwise
	 */
	public Path getPathFromFileName(String fileName) {
		if (listedFiles.containsKey(fileName)) {
			return listedFiles.get(fileName);
		}
		return null;
	} // getPathFromFileName
	
	/**
	 * Returns the names of all currently listed files.
	 * 
	 * @return List of all paths in the current listed files set.
	 */
	public List<Path> getSharedFiles() {
		return new ArrayList<Path>(listedFiles.values());
	} // getSharedFiles
	
	/**
	 * Test whether the supplied filePath is in the shared directory or not.
	 * 
	 * @param filePath the path to the file in question.
	 * @return true if the file is in the shared directory; false otherwise
	 */
	public boolean isFromSharedDir(Path filePath) {
		if (this.shareDir == null || filePath == null) {
			return false;
		}
		// Normalize to be certain of a fair comparison
		Path normShareDir = this.shareDir.normalize();
		Path normFilePath = filePath.normalize();
		return normFilePath.startsWith(normShareDir);
	} // isFromSharedDir

	/**
	 * Adds a file path to the set of registered files. This file will be shared
	 * with other consumers connected to the Web service and becomes available for
	 * download. If the path is currently block-listed, this will remove it from the
	 * excludedFiles set.
	 * 
	 * @param filePath the path of the file to be included for sharing.
	 */
	public void listFile(Path filePath) {
		excludedFiles.remove(filePath);
		userSharedFiles.add(filePath);
		try {
			FSConsumer.listFile(this.peerID, filePath.getFileName().toString(), this.localAddress, this.localPort);
		} catch (Exception e) {
			System.err.println("[SHARE] Register file failed: " + e.getMessage());
			return;
		}
		listedFiles.put(filePath.getFileName().toString(), filePath);
		notifyListeners();
	} // registerFile
	
	/**
	 * Stops sharing a file immediately and notifies the GUI of the change.
	 * 
	 * @param filePath the file that should no longer be shared.
	 */
	public void delistFile(Path filePath) {
		userSharedFiles.remove(filePath);
		try {
			FSConsumer.delistFile(filePath.getFileName().toString(), this.peerID);
			listedFiles.remove(filePath.getFileName().toString());
			notifyListeners();
		} catch (Exception e) {
			System.err.println("[SHARE] Immediate delist failed.");
		}
	} // delistFile

	/**
	 * Indicates that a file should not be shared by block-listing its file path.
	 * Block listing is required to allow the user to stop sharing files that are in
	 * a configured share directory if they wish.
	 * 
	 * @param filePath the path of the file that is not to be shared.
	 */
	public void excludeFile(Path filePath) {
		excludedFiles.add(filePath);
		delistFile(filePath);
	} // excludeFile

	/**
	 * Creates a new scheduled thread to periodically scan the share directory for
	 * changes.
	 * 
	 * @param int The number of seconds between heartbeats and updates of the share
	 *            directory.
	 */
	public void startMonitoring(int period) {
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleAtFixedRate(this::syncFiles, 0, period, TimeUnit.SECONDS);
	}
	
	/**
	 * Shuts down the scheduler immediately to stop the monitoring thread.
	 */
	public void stop() {
		if (scheduler != null) {
			scheduler.shutdownNow();
			try {
				if (!scheduler.awaitTermination(3, TimeUnit.SECONDS)) {
					System.err.println("[ERROR] ShareManager Monitor did not terminate gracefully.");
				}
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
			}
		}
	} // stop
	
	/**
	 * Add a new listener to the set of listeners that require notification of when the set
	 * of listed files changes.
	 * 
	 * @param listener the new listener.
	 */
	public void addUpdateListener(ShareUpdateListener listener) {
		this.listeners.add(listener);
	}
	
	/**
	 * Notifies each listener of the update to the listed files set.
	 */
	private void notifyListeners() {
		for (ShareUpdateListener l : listeners) {
			l.onShareListChanged();
		}
	}

	/**
	 * Scans each of the files in the shared directory and processes them so long as
	 * they are regular files and they are not hidden. Additionally responsible for
	 * sending a heartbeat signal to the server to indicate that the client is still
	 * present and providing files.
	 * 
	 * This method must be synchronized because setShareDir() will immediately call
	 * a syncFiles action to update the files being shared. If this happens at the
	 * same time as the worker thread that calls this method, it can lead to a race
	 * condition. A dirty bit (changePending) is used to indicate to this method
	 * that a change occurred while it was running so that it can immediately update
	 * the files again.
	 */
	private void syncFiles() {
		
		/*
		 * Give up if this method is being used by another thread. The dirty bit will be
		 * set whenever shareDir changes which will cause the thread that possesses the
		 * lock to perform another update before it leaves the method.
		 */
		if (!syncLock.tryLock()) {
			return;
		}

		try {
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
				changePending = false;
				try {
					Set<Path> intendedFiles = new HashSet<>();

					/*
					 * Add any regular files present in a configured share directory to the set of
					 * files intended for sharing
					 */
					if (this.shareDir != null && Files.exists(this.shareDir)) {
						try (Stream<Path> stream = Files.list(this.shareDir)) {
							stream.filter(Files::isRegularFile).filter(p -> {
								try {
									return !Files.isHidden(p);
								} catch (IOException ioe) {
									return false;
								}
							}).forEach(intendedFiles::add);
						}
					}

					/*
					 * intendedFiles is the union of files in the share directory and files
					 * explicitly set as shared by the user minus any block-listed files
					 */
					intendedFiles.addAll(listedFiles.values());
					intendedFiles.addAll(userSharedFiles);
					intendedFiles.removeAll(excludedFiles);

					/* Share any files that are intended for sharing but are not currently listed */
					for (Path p : intendedFiles) {
						String fileName = p.getFileName().toString();
						Path currentPath = listedFiles.get(fileName);

						// Check if a file with the same name is already in the map
						// If its already in the map, replace it
						if (currentPath == null || !currentPath.equals(p)) {
							if (currentPath != null) {
								try {
									FSConsumer.delistFile(fileName, this.peerID);
								} catch (Exception e) {
									/* Best effort made */
								}
							}

							// List the new or updated path
							if (!listedFiles.containsKey(fileName) || !listedFiles.get(fileName).equals(p)) {
								FSConsumer.listFile(this.peerID, p.getFileName().toString(), this.localAddress,
										this.localPort);
								listedFiles.put(p.getFileName().toString(), p);
								System.out.println("[SHARE] Registered " + p + " with service.");
							}
						}
					}

					/* De-list any departing files (those that are listed but no longer intended) */
					Iterator<Map.Entry<String, Path>> it = listedFiles.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, Path> entry = it.next();
						Path p = entry.getValue();
						String fileName = entry.getKey();

						if (!intendedFiles.contains(p) || !Files.isReadable(p)) {
							FSConsumer.delistFile(fileName, this.peerID);
							// Need to remove the file from intendedFiles if it is unreadable
							intendedFiles.remove(p);
							it.remove();
							System.out.println("[SHARE] De-listed " + p);
						}
					}
					
					/*
					 * Send the heartbeat and detect if the server has lost the file listings. If
					 * the server has reaped the listed files between heartbeats due to a delay,
					 * then this check will cause an immediate refresh of the share directory
					 */
					if (!listedFiles.isEmpty() && !FSConsumer.keepAlive(this.peerID)) {
						System.out.println("[SHARE] Session lost. Attempting immediate re-sync...");
						listedFiles.clear();
						// Limit the number of retries to 3
						needsRetry = (retries++ < 3);
					}

				} catch (IOException ioe) {
					System.err.println("[SHARE] Directory sync error " + ioe.getMessage());
					ioe.printStackTrace();
				} catch (WebServiceException we) {
					System.err.println("[SHARE] Error connecting the the service: " + we.getMessage());
				}
			} while (needsRetry || changePending);
		} finally {
			syncLock.unlock();
			notifyListeners();
		}
	} // syncDirectory
}
