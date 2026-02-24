/**
 * title: File Share Client GUI
 * @author Dominic Evans
 * @date February 16, 2026
 * @version 1.0
 * @copyright 2026 Dominic Evans
 */

/**
 * Implements the GUI for File Share Client. This GUI presents a window with a text console for
 * logging messages from the client and service as well as two tabs:
 * 
 *   1. A file searching/download tab for finding files and downloading them from peers.
 *   2. A file sharing tab that lists currently shared files and allows for adding/removing
 *      files via a file picker.
 *      
 * This class is instantiated on a GUI thread by FSClient and calls methods from FSClientConsumer.
 */

package fileshare.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.JScrollPane;

import fileshare.generated.FileInfo;
import fileshare.transport.FileRequester;
import jakarta.xml.ws.WebServiceException;

public class FSClientGUI extends JFrame {

	private static final long serialVersionUID = 1L;

	private String peerID;
	private Path downloadDir;
	private ShareManager manager;
	private DefaultTableModel searchTableModel;
	private JTable searchFileTable;
	private JTextField searchField;
	private JButton searchBtn;
	private JButton downloadBtn;
	private JTextField shareDirField;
	private JButton browseDirBtn;
	private JButton stopSharingDirBtn;
	private DefaultListModel<Path> sharedFilesListModel;
	private JList<Path> sharedFilesList;
	private JButton shareFileBtn;
	private JButton stopSharingBtn;
	private JTextArea consoleArea;

	public FSClientGUI(String peerID, Path downloadDir, ShareManager manager) {
		/* Initialize members */
		this.peerID = peerID;
		this.downloadDir = downloadDir;
		this.manager = manager;

		/* Set up the window */
		initGUI(this.peerID);

		/* Configure the action listeners */
		initListeners();

		/* Redirect stdout and stderr to the GUI console for logging */
		redirectSystemStreams();
	} // ctor
	
	/**
	 * Creates a new error popup window to alert the user of some important failure.
	 * This should be used only for first-class errors that cause significant issues
	 * with program function.
	 * 
	 * @param errTitle the title that will be displayed on the popup window.
	 * @param message  the message that will be conveyed to the user.
	 * @param fatal    boolean indicating whether or not the program should
	 *                 terminate. If true, the program will terminate as soon as the
	 *                 message is acknowledged, otherwise, it will simply show the
	 *                 message and the window will close
	 */
	public static void showErrorPopup(String errTitle, String message, boolean fatal) {
		// Check if a shutdown is in progress; this avoids massive window proliferation
		if (FSClient.isShuttingDown()) {
			return;
		}
		// Show a window and shut down the program if the error is fatal
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(null, message, errTitle, JOptionPane.ERROR_MESSAGE);
			if (fatal) {
				FSClient.shutdown();
			}
		});
	} // showErrorPopup

	/**
	 * Responsible for initializing the various GUI components with values. The GUI
	 * is composed of three main sections: a title bar, a central tabbed file table,
	 * and a reporting console at the bottom. The control elements on the search tab
	 * include a search bar and button to executed searches as well as a download
	 * button that will download the file selected in the file table. The control
	 * elements of the share tab include a button for selecting the share directory,
	 * if desired, and a button for listing files via a file picker. There is
	 * additionally a button for de-listing files when they are selected from the
	 * list of shared files found on this tab.
	 * 
	 * @param peerID the UUID that identifies the current client. This will be
	 *               displayed on the top bar of the rendered window.
	 */
	private void initGUI(String peerID) {
		// Set up the window
		setTitle("P2P File Share - " + peerID);
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(5, 5));

		// North: header with Peer ID
		JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		northPanel.add(new JLabel("Peer ID: " + peerID));
		add(northPanel, BorderLayout.NORTH);

		// Center: Tabbed Pane
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Search", createSearchTab());
		tabbedPane.addTab("Share", createShareTab());
		add(tabbedPane, BorderLayout.CENTER);

		// South: Console
		this.consoleArea = new JTextArea(10, 20);
		this.consoleArea.setEditable(false);
		this.consoleArea.setBackground(Color.BLACK);
		this.consoleArea.setForeground(Color.WHITE);
		this.consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		add(new JScrollPane(this.consoleArea), BorderLayout.SOUTH);

	} // initGUI

	/**
	 * Helper method for initGUI. This method creates the GUI elements for the
	 * search tab.
	 * 
	 * @return JPanel object with all of the search tab GUI elements.
	 */
	private JPanel createSearchTab() {
		JPanel searchTab = new JPanel(new BorderLayout());

		// File list of search results controlled by the most recent search query
		String[] columnNames = { "File ID", "File Name" };
		this.searchTableModel = new DefaultTableModel(columnNames, 0);
		this.searchFileTable = new JTable(this.searchTableModel);
		searchTab.add(new JScrollPane(this.searchFileTable), BorderLayout.CENTER);

		// Controls for searching, includes a text box and button for searching
		JPanel searchControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
		this.searchField = new JTextField(30);
		this.searchBtn = new JButton("Search Files");
		this.downloadBtn = new JButton("Download");
		this.downloadBtn.setEnabled(false);

		searchControls.add(new JLabel("Query:"));
		searchControls.add(this.searchField);
		searchControls.add(this.searchBtn);
		searchControls.add(this.downloadBtn);
		searchTab.add(searchControls, BorderLayout.SOUTH);

		return searchTab;
	}

	/**
	 * Helper method for initGUI. This method creates the GUI elements for the share
	 * tab to show what files are currently being shared and for controlling which
	 * files are shared.
	 * 
	 * @return JPanel object with all the share tab GUI elements.
	 */
	private JPanel createShareTab() {
		JPanel shareTab = new JPanel(new BorderLayout());

		/*
		 * Directory panel that shows the shared directory and gives a button for
		 * changing what directory is shared.
		 */
		JPanel dirPanel = new JPanel(new BorderLayout(5, 5));
		dirPanel.setBorder(BorderFactory.createTitledBorder("Sharing Directory"));

		this.shareDirField = new JTextField("No directory selected");
		this.shareDirField.setEditable(false);
		this.browseDirBtn = new JButton("Share Directory");
		this.stopSharingDirBtn = new JButton("Unlist Directory");
		
		// Enable the stop sharing directory button if there is a saved share directory
		// in the configuration
		if (this.manager.getShareDir() == null) {
			this.stopSharingDirBtn.setEnabled(false);
		} else {
			this.stopSharingDirBtn.setEnabled(true);
		}
		
		JPanel shareDirControlsPanel = new JPanel(new GridLayout(1, 2, 5, 0));

		dirPanel.add(this.shareDirField, BorderLayout.CENTER);
		shareDirControlsPanel.add(this.browseDirBtn);
		shareDirControlsPanel.add(this.stopSharingDirBtn);
		dirPanel.add(shareDirControlsPanel, BorderLayout.EAST);
		shareTab.add(dirPanel, BorderLayout.NORTH);

		/*
		 * List of the files that are currently being shared is in the middle of the
		 * screen with buttons below it for adding and removing files from the list.
		 */
		this.sharedFilesListModel = new DefaultListModel<Path>();
		this.sharedFilesList = new JList<Path>(this.sharedFilesListModel);
		shareTab.add(new JScrollPane(this.sharedFilesList), BorderLayout.CENTER);

		JPanel shareControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
		this.shareFileBtn = new JButton("Share File");
		this.stopSharingBtn = new JButton("Stop Sharing");
		this.stopSharingBtn.setEnabled(false);
		shareControls.add(this.shareFileBtn);
		shareControls.add(this.stopSharingBtn);
		shareTab.add(shareControls, BorderLayout.SOUTH);

		return shareTab;
	}

	/**
	 * Connects the elements of the GUI with their listeners which will activate the
	 * prescribed method when interacted with. Responsible for ensuring that all
	 * buttons do what they should and that text input boxes respond accordingly,
	 * etc.
	 */
	private void initListeners() {
		/* Observer for ShareManager changes */
		this.manager.addUpdateListener(() -> {
			SwingUtilities.invokeLater(this::updateSharedFilesList);
		});

		/* Search */
		this.searchBtn.addActionListener(e -> handleSearch());
		this.searchField.addActionListener(e -> this.searchBtn.doClick());
		this.downloadBtn.addActionListener(e -> handleDownload());

		// Control the activation of the download button depending on whether or not a
		// file is selected in the UI
		this.searchFileTable.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				int selectedRow = this.searchFileTable.getSelectedRow();
				
				// Guard against triggering this listener when the table is cleared
				// this occurs when a new search is entered
				if (selectedRow < 0) {
					this.downloadBtn.setEnabled(false);
					return;
				}
				
				// Activate the download button only if there is something selected
				// and that selection corresponds to a valid file
				String fileID = this.searchTableModel.getValueAt(selectedRow, 0).toString();
				this.downloadBtn.setEnabled(selectedRow != -1 && !fileID.equals("N/A"));
			}
		});

		/* Share */
		this.shareFileBtn.addActionListener(e -> handleShareFile());
		this.stopSharingBtn.addActionListener(e -> handleStopSharing());
		this.browseDirBtn.addActionListener(e -> handleChangeShareDir());
		this.stopSharingDirBtn.addActionListener(e -> handleStopSharingDir());

		this.sharedFilesList.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
				Path selectedFile = sharedFilesList.getSelectedValue();
				this.stopSharingBtn.setEnabled(selectedFile != null);
			}
		});
	} // initActionListeners

	/**
	 * Creates a new anonymous OutputStream class for redirecting stdout and stderr
	 * to the GUI console rather than to the system console.
	 */
	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) {
				updateConsole(String.valueOf((char) b));
			}

			@Override
			public void write(byte[] b, int off, int len) {
				updateConsole(new String(b, off, len));
			}
		};
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	} // redirectSystemStreams

	/**
	 * Responsible for updating the JTextArea of the console with any outputs in a
	 * thread-safe manner.
	 * 
	 * @param text The output to be printed to the console
	 */
	private void updateConsole(String text) {
		SwingUtilities.invokeLater(() -> {
			this.consoleArea.append(text);
			this.consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
		});
	} // updateConsole
	
	/**
	 * Updates the list of shared files. Called from action listeners whenever this list
	 * is liable to change.
	 */
	private void updateSharedFilesList() {
		this.sharedFilesListModel.clear();
		List<Path> sharedFiles = manager.getSharedFiles();
		for (Path p : sharedFiles) {
			this.sharedFilesListModel.addElement(p);
		}
	} // updateSharedFilesList
	
	/*
	 * Handlers: each of these methods execute the various methods of the program.
	 * Each is named as handle<GUIElement> indicating which GUI element they are
	 * associated with.
	 */
	private void handleSearch() {
		String query = this.searchField.getText();
		// Be sure that the search actually has some text in it
		if (query.trim().isEmpty()) {
			return;
		}

		// Disable the search button while a search thread is active to
		// limit thread generation
		this.searchBtn.setEnabled(false);
		new Thread(() -> {
			try {
				System.out.println("[SYS] Querying server for: " + query);
				List<FileInfo> results = FSConsumer.searchFiles(query);
				if (results == null) {
					System.err.println("[SYS] Search failed.");
					return;
				}
				
				// Update UI with results
				SwingUtilities.invokeLater(() -> {
					this.searchTableModel.setRowCount(0);
					// Check if any results were returned for a search; if not, display a message in
					// the results table
					if (results.isEmpty()) {
						this.searchTableModel.addRow(new Object[] { "N/A", "No files found for '" + query + "'" });
						System.out.println("[SYS] Search returned with 0 results.");
					} else {
						for (FileInfo fi : results) {
							this.searchTableModel.addRow(new Object[]{ fi.getFileID(), fi.getFileName() });
						}
					}
				});
			} catch(WebServiceException we) {
				System.err.println("Search could not connect to the remote service.");
			} catch (Exception e) {
				System.err.println("[ERROR] Search failed " + e.getMessage());
			} finally {
				this.searchBtn.setEnabled(true);
			}
		}).start();
	} // handleSearch

	private void handleDownload() {
		// Get the row number and return if no valid row is selected
		int row = this.searchFileTable.getSelectedRow();
		if (row == -1) {
			return;
		}

		/*
		 * Check that the row is not being used for messaging the user. This occurs when
		 * a search returns a "file not found" rather than a file name to the file
		 * table.
		 */
		String fileIDStr = this.searchTableModel.getValueAt(row, 0).toString();
		if (fileIDStr.equals("N/A")) {
			return;
		}
		
		// Stop the user from creating a new thread before the download thread returns
		this.downloadBtn.setEnabled(false);
		
		// Execute a new thread to download the file to keep the GUI responsive
		new Thread(() -> {
			try {
				// Parse the file ID from the table
				int fID = Integer.parseInt(fileIDStr);
				FileInfo finfo = FSConsumer.getFileOwner(fID);
				
				// Download the file
				if (finfo != null) {
					String fileName = finfo.getFileName();
					System.out.println("Requesting " + fileName);
					FileRequester.downloadFile(finfo, this.downloadDir);
				} else {
					System.err.println( "[ERROR] Could not retrieve owner details for file '" + 
				                         fileIDStr + "' from server.");
				}
			} catch (NumberFormatException nfe) {
				System.err.println("[ERROR] Download thread failed - Invalid File ID: " + nfe.getMessage());
			} catch (Exception e) {
				System.err.println("[ERROR] Download thread failed " + e.getMessage());
			} finally {
				this.downloadBtn.setEnabled(true);
			}
		}).start();
	} // handleDownload

	private void handleShareFile() {
		// Create a file chooser to allow the user to select a file to share
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select New File to Share");
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		
		// Check that a valid file was chosen and add it to the list of files to
		// share with other users.
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			// List the file with the share manager
			try {
				Path newFile = chooser.getSelectedFile().toPath();
				// Be certain the file is readable
				if (Files.isReadable(newFile)) {
					this.manager.listFile(newFile);
				} else {
					System.err.println("ERROR: '" + newFile.getFileName() + "' is not readable or does not exist.");
				}
			} catch (WebServiceException we) {
				System.err.println("Share error: could not connect to the remote service.");
			}
		}
	} // handleShareFile

	private void handleChangeShareDir() {
		// Create a file chooser, restricted to selecting directories
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle("Select New Sharing Directory");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		
		// If a valid directory is chosen, update the share directory
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			// Update the text field to the new share directory
			Path newSharePath = chooser.getSelectedFile().toPath();
			this.shareDirField.setText(newSharePath.toAbsolutePath().toString());
			
			// Update the ShareManager for the new directory
			this.manager.setShareDir(newSharePath);
			this.stopSharingDirBtn.setEnabled(true);
			System.out.println("Switched sharing directory to: " + newSharePath.getFileName());
		}
	} // handleChangeShareDir
	
	private void handleStopSharingDir() {
		this.manager.setShareDir(null);
		this.shareDirField.setText("No directory selected");
		this.stopSharingDirBtn.setEnabled(false);
	} // handleStopSharingDir

	private void handleStopSharing() {
		Path filePath = this.sharedFilesList.getSelectedValue();
		if (filePath == null) {
			return;
		}

		// Stop sharing the file. If the file is from a shared directory, exclude it
		this.stopSharingBtn.setEnabled(false);
		new Thread(() -> {
			try {
				if (this.manager.isFromSharedDir(filePath)) {
					this.manager.excludeFile(filePath);
				} else {
					this.manager.delistFile(filePath);
				}
				System.out.println("Stopped sharing: " + filePath.getFileName());
			} finally {
				this.stopSharingBtn.setEnabled(false);
			}
		}).start();
	} // handleStopSharing
}
