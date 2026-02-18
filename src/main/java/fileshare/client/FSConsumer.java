/**
 * title: File Share Consumer
 * @author Dominic Evans
 * @date February 16, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * This class implements the singleton pattern to create a single port for connections
 * with the remote service. All other classes use this one to make remote requests of
 * the file sharing database and to receive data from the database as the result of
 * queries. As a natural way of implementing this, this class is composed purely of
 * static methods to permit easy use by other objects in the client system.
 */

package main.java.fileshare.client;

import java.util.List;

import main.java.fileshare.generated.FileInfo;
import main.java.fileshare.generated.FileShareService;
import main.java.fileshare.generated.FileShareWS;
import jakarta.xml.ws.BindingProvider;

public class FSConsumer {

	private static FileShareWS port;
	private static String endpointURL;

	// Guard: this class shall not be instantiated as an object.
	private FSConsumer() {}

	/**
	 * Permits the reconfiguration of the consumer to point at a new end point.
	 * 
	 * @param url the location of the new end point.
	 */
	public static void setEndpoint(String url, String newPort) {
		endpointURL = "http://" + url + ":" + newPort + "/FileShare_Service/FileShareService?wsdl";
		port = null;
	}

	/**
	 * Resolves the port that corresponds to the currently configured end point.
	 * 
	 * @return FileShareWS port that enables contacting the target end point.
	 */
	private static synchronized FileShareWS getPort() {
		// The URL must be set before the first time a remote service is called
		if (endpointURL == null) {
			String errmsg = "Must set URL via FSConsumer.setEndpoint() before any FSConsumer member may be utilized.";
			FSClientGUI.showErrorPopup("NO URL SET", errmsg, true);
		}

		if (port == null) {
			try {
				FileShareService service = new FileShareService();
				port = service.getFileShareWSPort();

				BindingProvider bp = (BindingProvider) port;
				bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpointURL);
			} catch (Exception e) {
				System.err.println("[FSConsumer] Connection error: " + e.getMessage());
				e.printStackTrace();
				String errmsg = "[FSConsumer] Failed to connect to service at " + endpointURL; 
				FSClientGUI.showErrorPopup("Connection Failed", errmsg, false);
			}
		}
		return port;
	}

	/**
	 * Wrapper for calling the listFile operation on the port for the remote
	 * service. This operation makes a file available for sharing by listing it with
	 * the file sharing database which in turn makes it discoverable for other peers
	 * who wish to download it.
	 * 
	 * @param ownerID   the UUID of the client, indicating file ownership.
	 * @param fileName  the name of the file being shared.
	 * @param ownerIP   the IP address of the local client.
	 * @param ownerPort the port over which peer file transfers will occur.
	 */
	public static void listFile(String ownerID, String fileName, String ownerIP, int ownerPort) {
		FileShareWS service = getPort();
		if (service == null) {
			System.err.println("[FSConsumer] Cannot listFile: service port is null.");
			return;
		}
		service.listFile(ownerID, fileName, ownerIP, ownerPort);
	}

	/**
	 * Wrapper for calling the delist file operation on the port for the remote
	 * service. This operation removes a file from the database, indicating that it
	 * is no longer available for sharing.
	 * 
	 * @param fileName the name of the file that is no longer being shared.
	 * @param peerID   the UUID of the client which serves as a stamp of ownership
	 *                 on the file.
	 */
	public static void delistFile(String fileName, String peerID) {
		FileShareWS service = getPort();
		service.delistFile(fileName, peerID);
	}

	/**
	 * Interacts with the remote Web service to perform a search of the database of
	 * available shared files
	 * 
	 * @param query string file name that the user wishes to find in the database.
	 * @return FileInfo[] an array of information about files. This array shall
	 *         contain FileInfo objects with ownerIP and ownerPort set to null to
	 *         preserve owner privacy.
	 */
	public static List<FileInfo> searchFiles(String query) {
		FileShareWS service = getPort();
		return service.searchFiles(query);
	}

	/**
	 * Wrapper for calling the getFileOwner operation on the port for the remote
	 * service. This operation returns the detailed information about a file that is
	 * required for instigating a peer-to-peer transfer of that file from its owner
	 * to the local client.
	 * 
	 * @param fileID the ID assigned to the file by the file sharing database
	 *               acquired by a previous search.
	 * @return FileInfo detailed information including the owner's IP and port
	 *         necessary for effecting the file transfer.
	 */
	public static FileInfo getFileOwner(int fileID) {
		FileShareWS service = getPort();
		return service.getFileOwner(fileID);
	}

	/**
	 * Wrapper for calling the getTTL operation on the port for the remote service.
	 * This operation returns the maximum age of a file on the file sharing database
	 * before it is considered stale. Any stale file is a candidate for the database
	 * reaper, which will remove it on its next scheduled execution.
	 * 
	 * @return int number of seconds that a file may be in the file share database
	 *         before it becomes stale.
	 */
	public static int getTTL() {
		FileShareWS service = getPort();
		if (service == null) {
			System.err.println("[FSConsumer] Cannot get TTL: service port is null.");
			return 60;
		}
		return service.getTTL();
	}

	/**
	 * Wrapper for calling the keepAlive operation on the port for the remote
	 * service. This operation acts as a heartbeat, indicating that all files
	 * associated with the client are still being actively shared. This will reset
	 * their age in the file share database and preserve them from being reaped.
	 * 
	 * @param clientID the UUID of the client that owns the file
	 * @return boolean true if at least one file was updated; false otherwise.
	 */
	public static boolean keepAlive(String clientID) {
		FileShareWS service = getPort();
		return service.keepAlive(clientID);
	}

	/**
	 * Wrapper for calling the disconnect operation on the port for the remote
	 * service. This operation indicates to the file sharing database that the
	 * client is shutting down and all of their files should be immediately removed
	 * from the database.
	 * 
	 * @param clientID the UUID of the client that is shutting down.
	 */
	public static void disconnect(String clientID) {
		FileShareWS service = getPort();
		service.disconnect(clientID);
	}

}
