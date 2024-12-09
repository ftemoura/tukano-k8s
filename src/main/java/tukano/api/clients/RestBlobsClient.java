package tukano.api.clients;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.rest.RestBlobs;
import tukano.impl.Token;
import utils.ConfigLoader;

public class RestBlobsClient extends RestClient implements Blobs {

    public RestBlobsClient() {
        super(ConfigLoader.getInstance().getBlobsInternalEndpoint(), RestBlobs.PATH);
    }

    public RestBlobsClient(String serverURI) {
        super(serverURI, RestBlobs.PATH);
    }

    private Result<Void> _delete(SecurityContext sc, String blobId) {
        return super.toJavaResult(
                target.path(blobId)
                        .request()
                        .cookie("AUTH", sc.getUserPrincipal().getName())
                        .delete());
    }

    private Result<Void> _deleteAllBlobs(SecurityContext sc, String userId) {
        return super.toJavaResult(
                target.path(userId)
                        .path(RestBlobs.BLOBS)
                        .request()
                        .cookie("AUTH", sc.getUserPrincipal().getName())
                        .delete());
    }

    @Override
    public Result<Void> upload(String blobId, byte[] bytes, String token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<byte[]> download(String blobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> delete(SecurityContext sc, String blobId) {
        return super.reTry( () -> _delete(sc, blobId));
    }

    @Override
    public Result<Void> deleteAllBlobs(SecurityContext sc, String userId) {
        return super.reTry( () -> _deleteAllBlobs(sc, userId));
    }
}