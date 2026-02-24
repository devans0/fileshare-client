# fileshare-client
A client for consuming the fileshare-ws web service and enabling peer-to-peer 
file sharing using information retrieved there.

# Dependencies
This project requires Apache Maven for the best experience building and packaging
the client. Development and testing was accomplished using Maven 3.9.12.

Java 17+ is required as the program targets the jakarta namespace over the javax
namespace for Web service interface. Please note that all development and testing
was accomplished using Java 17 and the functionality of the program while using
other version is not assured.

In order for the program to compile, the fileshare-ws project must first be compiled 
and deployed on a locally running Wildfly server. This is required only for the 
compilation and packaging via Maven, with the resulting jar able to connect to 
an arbitrary instance of the fileshare-ws Web service.

# Build
First, start the Wildfly server and deploy and instance of the fileshare-ws Web
service. This is covered in the README.md for that project.

If the client will only be used from the command line, it is sufficient to compile
the project with the following command, executed from the project root:

    mvn clean compile

The client may also be packaged into a jar that contains all requisite libraries
using the command:

    mvn clean package

If a jar is packaged using this command then the result is found in the target/ 
directory under the name fileshare-client-1.0.jar. It is recommended to package
the client into a jar as this makes testing far simpler since sandboxing the 
application becomes trivial.

# Running
If no jar was packaged then running the the client is accomplished through the
command:

    mvn exec:java -Dexec.mainClass="fileshare.client.FSClient"

If a jar was packaged then it may be run using the command:

    java -jar target/fileshare-client-1.0.jar

Alternatively, a file explorer may be used the jar may be launched in the same
manner as any other executable file.

Regardless of means of execution, the client may be configured through the use of
a client.properties file. This file is not required, but having a client.properties
file in the current working directory when the client launches will allow for
manipulating the settings of the client. This includes setting the location of the
Web service server that the client should query, changing the share port, setting
a share and download directory, along with other options. By default, there is no
share directory included, but this option may be configured by adding:

    client.share_dir=/path/to/directory

to the client.properties file.

# Configuration
As previously mentioned, configuration is achieved via the client.properties file.
This file contains several options that allow for configuring the client to operate
how you would like. The configuration options are:

- service.address: The URL for the location of the Web service, when it is running.
- service.port: The port on which the service is accepting connections.
- client.download_dir: Path to the desired download directory; destination for downloaded files.
- client.timezone: Set the timezone that the client will use; default is UTC, should match the service.
- client.identity_file: Path to the file that the client will store its UUID in for identifying itself to the service.
- client.max_simultaneous_connections: The number of simultaneous peers to share with at a time.
- client.share_port: The port over which the client will listen for new share requests.

# Usage
The client has the following capabilities:

1. Sharing individual selected files.
2. Sharing entire directories of files.
3. Downloading files from peer clients to the download directory.
4. Providing shared files to peer clients via a direct TCP socket connection.

### Sharing Files
To directly share a single file, first click on the "Share" tab at the top of the 
screen. Next, click the "Share File" button on the left just above the console. 
Then, choose a file using the file picker that pops up and click open to confirm
your selection. The file will be registered with the Web service for other peers
to discover and will appear in the shared files list in the central table of the
Share tab.

To stop sharing a file, click on the file that you wish to stop sharing from the
shared file list on the Share tab. Once the file is highlighted, click on the
"Stop Sharing" button. The file will be immediately removed from the Web service
and no other peers will be able to discover it. Any file transfers that are
currently underway for this file will complete.

### Sharing Directories
To share a directory of files, first click on the "Share" tab at the top of the
screen. Next, click on the "Share Directory" button located in the top right of the window.
Then, select a directory from the provided file picker and click "Open" to confirm
your choice. Every regular file (i.e. every file that is not a directory, hidden,
or otherwise unaccessible) will be immediately registered with the Web service
for discovery by other peers. This selection will persist: upon closing the client,
your selection will be saved in the client.properties file so that the selected
directory will be re-listed with the Web service the next time the client is started
if the directory still exists.

To stop sharing a directory, click on the "Unlist Directory" button immediately to
the right of the "Share Directory" button. This will de-list all files that were shared
as the result of listing the currently active share directory. If there is no currently
active share directory then this button will not be available. The choice to de-list
a directory also persists. When a directory is de-listed, it is removed from the
client.properties and will not be shared again when the client next starts; in
order to share the directory again, select it again with the "Share Directory" button.

### Downloading Files
To download a file, first navigate to the "Search" tab near the top of the window.
Next, type a search into the "Query:" text box and click "Search". If there are any
files matching your query currently registered with the Web service, they will
appear in the table above the query text box, which will list their FileID (assigned by
the share service) and their file name. To download a file, click on the desired file
to highlight it and click the "Download" button. This will initiate a download and
the file will be transferred into whatever directory you have configured to be the
download directory.

### Providing Shared Files
Providing shared files to peers is an entirely passive operation. Once a file is
listed with the share service, simply leave the client open and it will listen for
connections from peer clients for share requests. When it receives such a request,
your local client will send that file over a TCP socket to the requester. This will
be indicated by a "Transfering file..." message printed to the console at the
bottom of the window.
