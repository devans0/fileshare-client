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

package fileshare.client;

import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import fileshare.generated.FileInfo;
import fileshare.generated.FileShareService;
import fileshare.generated.FileShareWS;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.WebServiceException;

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
	} // setEndpoint

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
				// 
				URL wsdlLocation = new URL(endpointURL);
				QName serviceName = new QName("http://service.ws.fileshare/", "FileShareService");
				FileShareService service = new FileShareService(wsdlLocation, serviceName);
				port = service.getFileShareWSPort();

				BindingProvider bp = (BindingProvider) port;
				String soapAddress = endpointURL.replace("?wsdl", "");
				bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, soapAddress);
			} catch (Exception e) {
				System.err.println("[FSConsumer] Connection error: " + e.getMessage());
				String errmsg = "[FSConsumer] Failed to connect to service at " + endpointURL; 
				FSClientGUI.showErrorPopup("Connection Failed", errmsg, false);
				return null;
			}
		}
		return port;
	} // getPort

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
	public static int listFile(String ownerID, String fileName, String ownerIP, int ownerPort) {
		FileShareWS service = getPort();
		if (service == null) {
			System.err.println("[FSConsumer] Cannot listFile: service port is null.");
			return -1;
		}
		try {
			return service.listFile(ownerID, fileName, ownerIP, ownerPort);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] list failed: Service unavailable.");
			return -1;
		}
	} // listFile

	/**
	 * Wrapper for calling the delist file operation on the port for the remote
	 * service. This operation removes a file from the database, indicating that it
	 * is no longer available for sharing.
	 * 
	 * @param fileID the ID of the file that is no longer being shared.
	 * @param peerID the UUID of the client which serves as a stamp of ownership on
	 *               the file.
	 */
	public static void delistFile(int fileID, String peerID) {
		FileShareWS service = getPort();
		if (service == null) {
			System.err.println("[FSConsumer] Cannot delistFile: service port is null");
			return;
		}
		try {
			service.delistFile(fileID, peerID);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] de-list failed: Service unavailable.");
		}
	} // delistFile

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
		if (service == null) {
			System.err.println("[FSConsumer] Cannot searchFiles: service port is null");
			return null;
		}
		try {
			return service.searchFiles(query);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] search failed: Service unavailable.");
			return null;
		}
	} //searchFiles

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
		if (service == null) {
			System.err.println("[FSConsumer] Cannot getFileOwner: service port is null");
			return null;
		}
		try {
			return service.getFileOwner(fileID);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] get file owner failed: Service unavailble.");
			return null;
		}
	} // getFileOnwer

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
		try {
			return service.getTTL();
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] get TTL failed: Service unavailable.");
			return 60;
		}
	} // getTTL

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
		if (service == null) {
			System.err.println("[FSConsumer] Cannot keepAlive: service port is null");
			return true;  // return true to avoid unnecessary looping in ShareManager.syncFiles()
		}
		try {
			return service.keepAlive(clientID);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] keep alive failed: Service is unavailable.");
			return true;
		}
	} // keepAlive

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
		if (service == null) {
			System.err.println("[FSConsumer] Cannot disconnect: service port is null");
			return;
		}
		System.out.println("Disconnecting from service...");
		try {
			service.disconnect(clientID);
		} catch (WebServiceException we) {
			System.err.println("[FSConsumer] disconnect failed: Service unavailable.");
		}
	} // disconnect

}
