
package fileshare.generated;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fileshare.generated package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DelistFile_QNAME = new QName("http://service.ws.fileshare/", "delistFile");
    private final static QName _DelistFileResponse_QNAME = new QName("http://service.ws.fileshare/", "delistFileResponse");
    private final static QName _Disconnect_QNAME = new QName("http://service.ws.fileshare/", "disconnect");
    private final static QName _DisconnectResponse_QNAME = new QName("http://service.ws.fileshare/", "disconnectResponse");
    private final static QName _FileInfo_QNAME = new QName("http://service.ws.fileshare/", "fileInfo");
    private final static QName _GetFileOwner_QNAME = new QName("http://service.ws.fileshare/", "getFileOwner");
    private final static QName _GetFileOwnerResponse_QNAME = new QName("http://service.ws.fileshare/", "getFileOwnerResponse");
    private final static QName _GetTTL_QNAME = new QName("http://service.ws.fileshare/", "getTTL");
    private final static QName _GetTTLResponse_QNAME = new QName("http://service.ws.fileshare/", "getTTLResponse");
    private final static QName _KeepAlive_QNAME = new QName("http://service.ws.fileshare/", "keepAlive");
    private final static QName _KeepAliveResponse_QNAME = new QName("http://service.ws.fileshare/", "keepAliveResponse");
    private final static QName _ListFile_QNAME = new QName("http://service.ws.fileshare/", "listFile");
    private final static QName _ListFileResponse_QNAME = new QName("http://service.ws.fileshare/", "listFileResponse");
    private final static QName _SearchFiles_QNAME = new QName("http://service.ws.fileshare/", "searchFiles");
    private final static QName _SearchFilesResponse_QNAME = new QName("http://service.ws.fileshare/", "searchFilesResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fileshare.generated
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link DelistFile }
     * 
     * @return
     *     the new instance of {@link DelistFile }
     */
    public DelistFile createDelistFile() {
        return new DelistFile();
    }

    /**
     * Create an instance of {@link DelistFileResponse }
     * 
     * @return
     *     the new instance of {@link DelistFileResponse }
     */
    public DelistFileResponse createDelistFileResponse() {
        return new DelistFileResponse();
    }

    /**
     * Create an instance of {@link Disconnect }
     * 
     * @return
     *     the new instance of {@link Disconnect }
     */
    public Disconnect createDisconnect() {
        return new Disconnect();
    }

    /**
     * Create an instance of {@link DisconnectResponse }
     * 
     * @return
     *     the new instance of {@link DisconnectResponse }
     */
    public DisconnectResponse createDisconnectResponse() {
        return new DisconnectResponse();
    }

    /**
     * Create an instance of {@link FileInfo }
     * 
     * @return
     *     the new instance of {@link FileInfo }
     */
    public FileInfo createFileInfo() {
        return new FileInfo();
    }

    /**
     * Create an instance of {@link GetFileOwner }
     * 
     * @return
     *     the new instance of {@link GetFileOwner }
     */
    public GetFileOwner createGetFileOwner() {
        return new GetFileOwner();
    }

    /**
     * Create an instance of {@link GetFileOwnerResponse }
     * 
     * @return
     *     the new instance of {@link GetFileOwnerResponse }
     */
    public GetFileOwnerResponse createGetFileOwnerResponse() {
        return new GetFileOwnerResponse();
    }

    /**
     * Create an instance of {@link GetTTL }
     * 
     * @return
     *     the new instance of {@link GetTTL }
     */
    public GetTTL createGetTTL() {
        return new GetTTL();
    }

    /**
     * Create an instance of {@link GetTTLResponse }
     * 
     * @return
     *     the new instance of {@link GetTTLResponse }
     */
    public GetTTLResponse createGetTTLResponse() {
        return new GetTTLResponse();
    }

    /**
     * Create an instance of {@link KeepAlive }
     * 
     * @return
     *     the new instance of {@link KeepAlive }
     */
    public KeepAlive createKeepAlive() {
        return new KeepAlive();
    }

    /**
     * Create an instance of {@link KeepAliveResponse }
     * 
     * @return
     *     the new instance of {@link KeepAliveResponse }
     */
    public KeepAliveResponse createKeepAliveResponse() {
        return new KeepAliveResponse();
    }

    /**
     * Create an instance of {@link ListFile }
     * 
     * @return
     *     the new instance of {@link ListFile }
     */
    public ListFile createListFile() {
        return new ListFile();
    }

    /**
     * Create an instance of {@link ListFileResponse }
     * 
     * @return
     *     the new instance of {@link ListFileResponse }
     */
    public ListFileResponse createListFileResponse() {
        return new ListFileResponse();
    }

    /**
     * Create an instance of {@link SearchFiles }
     * 
     * @return
     *     the new instance of {@link SearchFiles }
     */
    public SearchFiles createSearchFiles() {
        return new SearchFiles();
    }

    /**
     * Create an instance of {@link SearchFilesResponse }
     * 
     * @return
     *     the new instance of {@link SearchFilesResponse }
     */
    public SearchFilesResponse createSearchFilesResponse() {
        return new SearchFilesResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DelistFile }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DelistFile }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "delistFile")
    public JAXBElement<DelistFile> createDelistFile(DelistFile value) {
        return new JAXBElement<>(_DelistFile_QNAME, DelistFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DelistFileResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DelistFileResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "delistFileResponse")
    public JAXBElement<DelistFileResponse> createDelistFileResponse(DelistFileResponse value) {
        return new JAXBElement<>(_DelistFileResponse_QNAME, DelistFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Disconnect }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Disconnect }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "disconnect")
    public JAXBElement<Disconnect> createDisconnect(Disconnect value) {
        return new JAXBElement<>(_Disconnect_QNAME, Disconnect.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DisconnectResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link DisconnectResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "disconnectResponse")
    public JAXBElement<DisconnectResponse> createDisconnectResponse(DisconnectResponse value) {
        return new JAXBElement<>(_DisconnectResponse_QNAME, DisconnectResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FileInfo }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link FileInfo }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "fileInfo")
    public JAXBElement<FileInfo> createFileInfo(FileInfo value) {
        return new JAXBElement<>(_FileInfo_QNAME, FileInfo.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileOwner }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetFileOwner }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "getFileOwner")
    public JAXBElement<GetFileOwner> createGetFileOwner(GetFileOwner value) {
        return new JAXBElement<>(_GetFileOwner_QNAME, GetFileOwner.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetFileOwnerResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetFileOwnerResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "getFileOwnerResponse")
    public JAXBElement<GetFileOwnerResponse> createGetFileOwnerResponse(GetFileOwnerResponse value) {
        return new JAXBElement<>(_GetFileOwnerResponse_QNAME, GetFileOwnerResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTTL }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetTTL }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "getTTL")
    public JAXBElement<GetTTL> createGetTTL(GetTTL value) {
        return new JAXBElement<>(_GetTTL_QNAME, GetTTL.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetTTLResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link GetTTLResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "getTTLResponse")
    public JAXBElement<GetTTLResponse> createGetTTLResponse(GetTTLResponse value) {
        return new JAXBElement<>(_GetTTLResponse_QNAME, GetTTLResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KeepAlive }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KeepAlive }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "keepAlive")
    public JAXBElement<KeepAlive> createKeepAlive(KeepAlive value) {
        return new JAXBElement<>(_KeepAlive_QNAME, KeepAlive.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link KeepAliveResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link KeepAliveResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "keepAliveResponse")
    public JAXBElement<KeepAliveResponse> createKeepAliveResponse(KeepAliveResponse value) {
        return new JAXBElement<>(_KeepAliveResponse_QNAME, KeepAliveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListFile }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListFile }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "listFile")
    public JAXBElement<ListFile> createListFile(ListFile value) {
        return new JAXBElement<>(_ListFile_QNAME, ListFile.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ListFileResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ListFileResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "listFileResponse")
    public JAXBElement<ListFileResponse> createListFileResponse(ListFileResponse value) {
        return new JAXBElement<>(_ListFileResponse_QNAME, ListFileResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchFiles }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SearchFiles }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "searchFiles")
    public JAXBElement<SearchFiles> createSearchFiles(SearchFiles value) {
        return new JAXBElement<>(_SearchFiles_QNAME, SearchFiles.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SearchFilesResponse }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SearchFilesResponse }{@code >}
     */
    @XmlElementDecl(namespace = "http://service.ws.fileshare/", name = "searchFilesResponse")
    public JAXBElement<SearchFilesResponse> createSearchFilesResponse(SearchFilesResponse value) {
        return new JAXBElement<>(_SearchFilesResponse_QNAME, SearchFilesResponse.class, null, value);
    }

}
