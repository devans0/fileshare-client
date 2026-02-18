/**
 * title: File Transfer Handler
 * description: Serializes and transfers files from the file system over the designated socket.
 * @author Dominic Evans
 * @date January 29, 2026; February 17, 2026
 * @version 2.0
 * @copyright 2026 Dominic Evans
 */

/**
 * File transfer handler is a Runnable class that is responsible for sending a file over the provided socket.
 * In order to ensure that it is sending data that it is permitted to do so, it queries the ShareManager to
 * see if the requested file name exists in the set of listed files. Any request for a file that is not in that
 * set is denied.
 * 
 * If the file is found by the ShareManager, then the path for that file is returned and used as the data source
 * for the ensuing transfer. Prior to transferring a file, the FileTransferHandler will send the size of the
 * file as a long over the share socket followed immediately by the file data itself. The FileRequester class
 * shall always read a long value prior to receiving file data.
 * 
 * If a file is not sharable for any reason (not in the listed files set, not readable, or a directory), then
 * the size value sent will be set to the sentinel value of -1 indicating an error. The FileRequester receiving
 * the share shall check that the size is greater than zero to confirm that a file is coming. If this test fails
 * then the FileRequester shall immediately close the connection.
 */

package fileshare.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import fileshare.client.ShareManager;

public class FileTransferHandler implements Runnable {
	
	private final Socket shareSock;
	private final ShareManager shareManager;
	
	public FileTransferHandler(Socket sock, ShareManager manager) {
		this.shareSock = sock;
		this.shareManager = manager;
	} //ctor

	@Override
	public void run() {

		try (Socket s = this.shareSock;
			 DataInputStream in = new DataInputStream(s.getInputStream());
			 OutputStream out = s.getOutputStream()) {
				 
			// Receive the filename from the requesting peer
			String fileName = in.readUTF();

			// Resolve the Path ensuring that there are no unsafe character sequences
			// and that the file request has sharDir as a prefix
			Path targetFile = shareManager.getPathFromFileName(fileName);
			
			/* 
			 * If the file does not exist in the share list, if that file is not readable or is a directory
			 * then the file cannot be shared. Send sentinel value of size = -1 to peer to indicate error.
			 * 
			 * Otherwise, send the size of the file before sending the file itself so that the remote
			 * peer knows what to expect.
			 */
			long size;
			if (targetFile == null || !Files.isReadable(targetFile) || Files.isDirectory(targetFile)) {
				size = -1;
				new DataOutputStream(out).writeLong(size);
				out.flush();
				System.err.println("[P2P] File not found or inaccessible: " + fileName);
			} else {
				System.out.println("[P2P] Sending " + fileName + " to " + s.getInetAddress());
				size = Files.size(targetFile);
				new DataOutputStream(out).writeLong(size);
				
				Files.copy(targetFile, out);
				out.flush();
				System.out.println("[P2P] " + fileName + " transfer complete.");
			}
		} catch (IOException ioe) {
			System.err.println("[P2P] Transfer error: " + ioe.getMessage());
		}
	} // run
}
