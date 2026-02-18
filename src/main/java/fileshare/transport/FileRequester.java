/**
 * title: File Requester
 * description: Retrieves files from other peers when supplied with a file name, IP address, and port
 * @author Dominic Evans
 * @date January 29, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

package main.java.fileshare.transport;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import main.java.fileshare.generated.FileInfo;

public class FileRequester {
	
	public static void downloadFile (FileInfo info, Path downloadDir) {
		String targetFileName = info.getFileName();
		String targetIP = info.getOwnerIP();
		int targetPort = info.getPort();

		System.out.println("[P2P] Attempting to download: " + targetFileName);
		
		// Be sure that the download directory exists
		try {
			if (!Files.exists(downloadDir)) {
				Files.createDirectories(downloadDir);
			}
		} catch (IOException ioe) {
			System.err.println("[P2P] Could not create download directory: " + ioe.getMessage());
			return;
		}
		
		// Create the connection to peer
		try (Socket sock = new Socket(targetIP, targetPort);
			 DataOutputStream out = new DataOutputStream(sock.getOutputStream());
			 DataInputStream in = new DataInputStream(sock.getInputStream())) {
			
			// Request the file by its name
			out.writeUTF(targetFileName);
			out.flush();

			// FileTransferHandler will provide an 8-byte header that indicates the size of the
			// incoming file prior to the file's bytes
			long fileSize = in.readLong();

			// Check if there was an issue on the remote peer; this can be extended to an error-code
			// system in the future.
			if (fileSize < 1) {
				throw new IOException("Remote peer error: could not retrieve " + targetFileName);
			}
			
			// File has been found
			System.out.println("[P2P] " + targetFileName + ": peer reported file size = " + fileSize + " bytes");
			
			// Prepare the local file path
			Path downloadPath = downloadDir.resolve(targetFileName);
			
			// Stream data from the socket into the destination file
			System.out.println("[P2P] " + targetFileName + ": receiving data...");
			long bytesCopied = Files.copy(in, downloadPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Check that the reported size matches the actual retrieved file
			if (bytesCopied != fileSize) {
				System.err.println("[WARN] " + targetFileName + " transfer mismatch! Expected " 
									+ fileSize + " bytes; received " 
									+ bytesCopied + " bytes.");
			} else {
				System.out.println("[P2P] " + targetFileName + " successfully transferred. Saved to: " + downloadPath);
			}
			
		} catch (IOException ioe) {
			System.err.println("[P2P] Download failed: " + ioe.getMessage());
		}
	}

}
