package nl.t42.openstack.command.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public abstract class AbstractCommand<M extends HttpRequestBase, N extends Object> {

    private HttpClient httpClient;

    protected M request;

    protected HttpResponse response;

    public AbstractCommand(HttpClient httpClient, String url, String token) {
        this.httpClient = httpClient;
        this.request = createRequest(url);
    }

    public AbstractCommand(HttpClient httpClient, String url) {
        this(httpClient, url, null);
    }

    public N execute() {
        try {
            response = httpClient.execute(request);
            HttpStatusChecker.verifyCode(getStatusCheckers(), response.getStatusLine().getStatusCode());
            N object = getReturnObject(response);
            EntityUtils.consume(response.getEntity());
            return object;
        } catch (IOException err) {
            throw new CommandException(500, CommandExceptionError.UNKNOWN, err);
        }
    }

    protected abstract M createRequest(String url);

    protected abstract HttpStatusChecker[] getStatusCheckers();

    protected N getReturnObject(HttpResponse response) throws IOException {
        return null; // returns null by default
    }
}
