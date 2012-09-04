package nl.t42.openstack.command.object;

import nl.t42.openstack.command.core.CommandException;
import nl.t42.openstack.command.core.CommandExceptionError;
import nl.t42.openstack.command.core.HttpStatusChecker;
import nl.t42.openstack.command.core.HttpStatusMatch;
import nl.t42.openstack.command.identity.access.Access;
import nl.t42.openstack.model.Container;
import nl.t42.openstack.model.StoreObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.*;

public class DownloadObjectCommand extends AbstractObjectCommand<HttpGet, byte[]> {

    public static final String ETAG             = "ETag";
    public static final String CONTENT_LENGTH   = "Content-Length";

    private File targetFile;

    public DownloadObjectCommand(HttpClient httpClient, Access access, Container container, StoreObject object) {
        super(httpClient, access, container, object);
    }

    public DownloadObjectCommand(HttpClient httpClient, Access access, Container container, StoreObject object, File targetFile) {
        this(httpClient, access, container, object);
        this.targetFile = targetFile;
    }

    @Override
    protected byte[] getReturnObject(HttpResponse response) throws IOException {
        String expectedMd5 = response.getHeaders(ETAG)[0].getValue();
        int contentLength = Integer.parseInt(response.getHeaders(CONTENT_LENGTH)[0].getValue());

        InputStream input = null;
        OutputStream output = null;
        try {
            input = response.getEntity().getContent();
            output = targetFile == null ? new ByteArrayOutputStream(contentLength) : new FileOutputStream(targetFile);
            byte[] buffer = new byte[65536];
            for (int length; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
            byte[] result = targetFile == null ? ((ByteArrayOutputStream)output).toByteArray() : new byte[] {};
            String realMd5 = targetFile == null ? DigestUtils.md5Hex(result) : getMd5OfFile();
            if (!expectedMd5.equals(realMd5)) {
                throw new CommandException(HttpStatus.SC_UNPROCESSABLE_ENTITY, CommandExceptionError.MD5_CHECKSUM);
            }
            return result;
        } finally {
            if (output != null) try { output.close(); } catch (IOException logOrIgnore) {}
            if (input != null) try { input.close(); } catch (IOException logOrIgnore) {}
        }
    }

    protected String getMd5OfFile() throws IOException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(targetFile);
            return DigestUtils.md5Hex(input);
        } finally {
            if (input != null) try { input.close(); } catch (IOException logOrIgnore) {}
        }
    }

    @Override
    protected HttpGet createRequest(String url) {
        return new HttpGet(url);
    }

    @Override
    protected HttpStatusChecker[] getStatusCheckers() {
        return new HttpStatusChecker[] {
            new HttpStatusChecker(new HttpStatusMatch(HttpStatus.SC_OK), null),
            new HttpStatusChecker(new HttpStatusMatch(HttpStatus.SC_NOT_FOUND), CommandExceptionError.CONTAINER_DOES_NOT_EXIST)
        };
    }

}
