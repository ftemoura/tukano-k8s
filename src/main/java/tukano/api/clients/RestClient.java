package tukano.api.clients;

import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;
import static tukano.api.Result.ErrorCode.TIMEOUT;

import java.net.URLEncoder;
import java.util.function.Supplier;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import tukano.impl.Token;
import utils.Sleep;

public class RestClient {
    private static Logger Log = Logger.getLogger(RestClient.class.getName());

    protected static final int READ_TIMEOUT = 10000;
    protected static final int CONNECT_TIMEOUT = 10000;

    protected static final int MAX_RETRIES = 3;
    protected static final int RETRY_SLEEP = 1000;

    final Client client;
    final String serverURI;
    final ClientConfig config;

    final WebTarget target;

    protected RestClient(String serverURI, String servicePath ) {
        this.serverURI = serverURI;
        this.config = new ClientConfig();

        config.property(ClientProperties.READ_TIMEOUT, READ_TIMEOUT);
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECT_TIMEOUT);

        this.client = ClientBuilder.newClient(config);
        this.target = client.target(serverURI).path( servicePath );
    }

    protected <T> Result<T> reTry(Supplier<Result<T>> func) {
        for (int i = 0; i < MAX_RETRIES; i++)
            try {
                return func.get();
            } catch (ProcessingException x) {
                x.printStackTrace();
                Log.fine("Timeout: " + x.getMessage());
                Sleep.ms(RETRY_SLEEP);
            } catch (Exception x) {
                x.printStackTrace();
                return Result.error(INTERNAL_ERROR);
            }
        System.err.println("TIMEOUT...");
        return Result.error(TIMEOUT);
    }

    protected Result<Void> toJavaResult(Response r) {
        try {
            var status = r.getStatusInfo().toEnum();
            System.out.println("STATUS: "+status);
            if (status == Status.OK && r.hasEntity()) {
                return ok(null);
            }
            else
            if( status == Status.NO_CONTENT) return ok();

            return error(errorCodeFromStatus(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    protected <T> Result<T> toJavaResult(Response r, Class<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            System.out.println("STATUS: "+status);
            if (status == Status.OK && r.hasEntity())
                return ok(r.readEntity(entityType));
            else
            if( status == Status.NO_CONTENT) return ok();

            return error(errorCodeFromStatus(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    protected <T> Result<T> toJavaResult(Response r, GenericType<T> entityType) {
        try {
            var status = r.getStatusInfo().toEnum();
            if (status == Status.OK && r.hasEntity())
                return ok(r.readEntity(entityType));
            else
            if( status == Status.NO_CONTENT) return ok();

            return error(errorCodeFromStatus(status.getStatusCode()));
        } finally {
            r.close();
        }
    }

    @Override
    public String toString() {
        return serverURI.toString();
    }
}