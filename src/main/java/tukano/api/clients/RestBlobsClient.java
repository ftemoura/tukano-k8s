package tukano.api.clients;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.rest.RestBlobs;
import tukano.impl.Token;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestBlobsClient extends RestClient implements Blobs {

    public RestBlobsClient(String serverURI) {
        super(serverURI, RestBlobs.PATH);
    }
//    private static String extractUserId(String url) {
//        Pattern pattern = Pattern.compile("/api/blobs/([a-zA-Z0-9]+)\\+");
//        Matcher matcher = pattern.matcher(url);
//
//        if (matcher.find()) {
//            return matcher.group(1);
//        }
//
//        return null;
//    }
    private Result<Void> _upload(String blobURL, byte[] bytes, String token) {
        return super.toJavaResult(
                target.path(blobURL)
                        .queryParam(RestBlobs.TOKEN, token)
                        .request()
                        //.cookie("AUTH", Token.get(Token.Service.BLOBS, extractUserId(blobURL) , Token.Role.ADMIN)) //TODO como é que vamos sacar o id sem splits?
                        .post( Entity.entity(bytes, MediaType.APPLICATION_OCTET_STREAM_TYPE)));
    }

    private Result<byte[]> _download(String blobURL, String token) {
        return super.toJavaResult(
                target.path(blobURL)
                        .queryParam(RestBlobs.TOKEN, token)
                        .request()
                        //.cookie("AUTH", Token.get(Token.Service.BLOBS, extractUserId(blobURL), Token.Role.ADMIN)) //TODO como é que vamos sacar o id sem splits?
                        .accept(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                        .get(), byte[].class);
    }

    private Result<Void> _delete(String blobURL, String token) {
        return super.toJavaResult(
                target.path(blobURL)
                        .queryParam(RestBlobs.TOKEN, token)
                        .request()
                        //.cookie("AUTH", Token.get(Token.Service.BLOBS, extractUserId(blobURL), Token.Role.ADMIN)) //TODO como é que vamos sacar o id sem splits?
                        .delete());
    }

    private Result<Void> _deleteAllBlobs(String userId, String token) {
        return super.toJavaResult(
                target.path(userId)
                        .path(RestBlobs.BLOBS)
                        .queryParam( RestBlobs.TOKEN, token )
                        .request()
                        //.cookie("AUTH", Token.get(Token.Service.BLOBS, userId, Token.Role.ADMIN))
                        .delete());
    }

    @Override
    public Result<Void> upload(String blobId, byte[] bytes, String token) {
        return super.reTry( () -> _upload(blobId, bytes, token));
    }

    @Override
    public Result<byte[]> download(String blobId, String token) {
        return super.reTry( () -> _download(blobId, token));
    }

    @Override
    public Result<Void> delete(String blobId, String token) {
        return super.reTry( () -> _delete(blobId, token));
    }

    @Override
    public Result<Void> deleteAllBlobs(String userId, String password) {
        return super.reTry( () -> _deleteAllBlobs(userId, password));
    }
}