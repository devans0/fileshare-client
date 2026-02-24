/**
 * title: File Share Client 
 * @author Dominic Evans
 * @date February 17, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * FSClient calls upon other classes in the program to render the GUI, set up the local 
 * environment, and perform any other tasks necessary for the program to function. Largely, 
 * it serves as a single point of entry into the client program. This class is responsible
 * for extracting, parsing, and passing any configuration options to other components so
 * that the user's desired functionality is achieved.
 * 
 * Initiation of the program follows these steps:
 *   - 
 * 
 * The client program as a whole consumes a remote Web service which in turn interfaces with
 * a database that contains listings of files that are available for sharing. The Web service
 * can be contacted in order to list new local files for sharing with other client programs,
 * searching for files that have been shared by other users, and for selecting and initiating
 * files for download.
 */

package fileshare.client;

import java.nio.file.Paths;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import fileshare.transport.PeerServer;
import fileshare.util.ConfigLoader;
import fileshare.util.IdentityManager;

public class FSClient {
	// Global State
	private static final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
	
	// Configuration Defaults
	private static final int DEFAULT_PORT = 2050;
	private static final String DEFAULT_TZ = "UTC";
	private static final String DEFAULT_DOWNLOAD_DIR = "download";
	
	// Configuration Actuals
	private static String peerID;
	private static Thread peerServerThread;
	private static Path downloadDir;
	private static Path shareDir;
	private static int localPort;
	
	// Runtime Objects
	private static ShareManager manager;

	public static void main(String[] args) {
		// Establish client identity and address
		peerID = IdentityManager.getUUID();
		String localAddress = getLocalIP();	
		localPort = ConfigLoader.getIntProperty("client.share_port", DEFAULT_PORT);	
		
		// Set the time zone
		String timezone = ConfigLoader.getProperty("client.timezone", DEFAULT_TZ);
		TimeZone.setDefault(TimeZone.getTimeZone(timezone));
		
		/*
		 * Configure a share and download directory. Note that the share directory may be 
		 * null indicating that the user has not configured a share directory in client.properties.
		 * This is only an initial configuration of the share directory as this may be changed
		 * via the GUI at any time by the user.
		 */
		String savedPath = ConfigLoader.getProperty("client.share_dir");
		shareDir = (savedPath != null) ? Paths.get(savedPath) : null;
		downloadDir = Paths.get(ConfigLoader.getProperty("client.download_dir", DEFAULT_DOWNLOAD_DIR));
		
		/*
		 * Command-line override of above configurations, not intended for regular use.
		 * This aids in testing by allowing a harness script to instantiate multiple
		 * clients with unique addresses, ports, and share/download directories.
		 * Otherwise, running multiple versions of the client would just have multiple
		 * processes accessing the same resources.
		 * 
		 * Override arguments must be supplied in the order: [port] [share-directory]
		 * [download-directory]
		 */
		if (args.length > 0) {
			localPort = Integer.parseInt(args[0]);
			shareDir = Paths.get(args[1]);
			downloadDir = Paths.get(args[2]);
		}
		
		// Ensure required directories exist and create the share manager
		initDirectories(shareDir, downloadDir);
		manager = new ShareManager(peerID, localAddress, shareDir, localPort);
		
		// Configure the consumer for the remote service; this will create the port to the service
		String endpointURL = ConfigLoader.getProperty("service.address");
		String endpointPort = ConfigLoader.getProperty("service.port");
		FSConsumer.setEndpoint(endpointURL, endpointPort);
		
		// Enable the shutdown hooks
		setShutdownHooks();
		
		// Start the GUI
		SwingUtilities.invokeLater(() -> {
			FSClientGUI gui = new FSClientGUI(peerID, downloadDir, manager);
			gui.setVisible(true);
			
			// Background thread that runs the peer server so long as the GUI is alive
			new Thread(() -> {
				// Set a sane heartbeat interval that is shorter than TTL to allow for network jitter
				int heartBeatPeriodSecs = Math.max(10, (int) 0.75 * FSConsumer.getTTL());
				manager.startMonitoring(heartBeatPeriodSecs);
				
				// Create the server thread for accepting peer connections and providing files
				int maxConnections = ConfigLoader.getIntProperty("client.max_simultaneous_connectcions", 4);
				PeerServer listener = new PeerServer(localPort, manager, maxConnections);
				peerServerThread = new Thread(listener);
				peerServerThread.setDaemon(true);
				peerServerThread.start();
			}).start();
		});
	} // main
	
	/**
	 * Uses a DatagramSocket hack to determine the local address of the client. This address will
	 * be broadcast to other clients as the location of any shared files.
	 * 
	 * @return String local host address.
	 */
	private static String getLocalIP() {
		try (DatagramSocket sock = new DatagramSocket()) {
			sock.connect(InetAddress.getByName("8.8.8.8"), 10002);
			return sock.getLocalAddress().getHostAddress();
		} catch (Exception e) {
			// Connection fails, return localhost
			return "127.0.0.1";
		}
	} // getLocalIP
	
	/**
	 * Initializes an arbitrary number of paths, ensuring that there is a directory
	 * for each one.
	 * 
	 * @param paths a variable number of paths, each of which must exist as a
	 *              directory on disk before this method returns.
	 */
	private static void initDirectories(Path... paths) {
		for (Path p : paths) {
			if (p == null) { continue; }
			try {
				if (Files.exists(p)) {
					continue;
				}
				Files.createDirectory(p);
				System.out.println("[SYS] Directory initialized: " + p);
			} catch (IOException ioe) {
				String errmsg = "Fatal error: could not initialize directory: " + p;
				System.err.println(errmsg);
				javax.swing.JOptionPane.showMessageDialog(null, 
														  errmsg, 
														  "Filesystem Error", 
														  javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		}
	} // initDirectories
	
	/**
	 * Allows other classes to determine if the program is actively shutting down.
	 * 
	 * @return boolean value of AtomicBoolean isShuttingDown.
	 */
	public static boolean isShuttingDown() {
		return isShuttingDown.get();
	}
	
	/**
	 * Performs shutdown operations to ensure that resources are gracefully closed and any persistent
	 * data is written to disk properly before the process exits.
	 */
	public static void shutdown() {
		/*
		 * If shutdown is called from some point in the program due to a fatal error, this
		 * method will eventually result in triggering the shutdown hook when System.exit is
		 * called at the bottom of this method. This guards against that shutdown hook
		 * executing this method redundantly and recursively.
		 */
		if (!isShuttingDown.compareAndSet(false, true)) {
			return;
		}
		
		System.out.println("[SYS] Initiating shutdown...");
		try {
			// De-list all currently shared files before the client goes offline
			FSConsumer.disconnect(peerID);
		} catch (Exception e) {
			System.err.println("[SYS] Could not notify FSConsumer service: " + e.getMessage());
		}
		
		// Stop the PeerServer thread to stop all sharing
		if (peerServerThread != null) {
			System.out.println("Shutting down peer server...");
			peerServerThread.interrupt();
		}

		// Stop the share manager worker thread and save the current share directory to properties
		System.out.println("Stopping share manager...");
		manager.stop();
		Path currSharePath = manager.getShareDir();
		String currShareDir = (currSharePath != null) ? currSharePath.toAbsolutePath().toString() : null;
		ConfigLoader.saveProperty("client.share_dir", currShareDir);

		/*
		 * Ready to exit.
		 * 
		 * If the current thread is the shutdown hook, avoid calling System.exit since this
		 * will be done at the end of the shutdown hook anyways. This avoids infinitely
		 * generating shutdown hooks by further System.exit calls.
		 */
		System.out.println("[SYS] Shutdown complete.");
		if (!Thread.currentThread().getName().equals("FSClient-Shutdown-Hook-Thread")) {
			System.exit(0);
		}
	} // shutdown
	
	/**
	 * Sets the shutdown hook
	 */
	private static void setShutdownHooks() {
		Thread shutdownThread = new Thread(() -> {
			shutdown();
		});
		// Set the name of the shutdown thread so that shutdown() can detect it
		shutdownThread.setName("FSClient-Shutdown-Hook-Thread");
		Runtime.getRuntime().addShutdownHook(shutdownThread);
	} // setShutdownHook
	
}
