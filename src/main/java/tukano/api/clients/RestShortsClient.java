package tukano.api.clients;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.rest.RestShorts;
import tukano.impl.Token;
import utils.Auth;
import utils.ConfigLoader;

public class RestShortsClient extends RestClient implements Shorts{

    public RestShortsClient() {
        super(ConfigLoader.getInstance().getShortsInternalEndpoint(), RestShorts.PATH);
    }

    public RestShortsClient(String serverURI) {
        super(serverURI, RestShorts.PATH);
    }


//    public Result<Short> _createShort(String userId) {
//        return super.toJavaResult(
//                target
//                        .path(userId)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .accept(MediaType.APPLICATION_JSON)
//                        .post( Entity.json(null)), Short.class);
//    }
//
//    public Result<Void> _deleteShort(String shortId) {
//        return super.toJavaResult(
//                target
//                        .path(shortId)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, shortId, Token.Role.ADMIN))
//                        .delete());
//    }

    public Result<Short> _getShort(String shortId) {
        return super.toJavaResult(
                target
                        .path(shortId)
                        .request()
                        .cookie("AUTH",Token.get(Token.Service.AUTH, shortId, Token.Role.USER))
                        .get(), Short.class);
    }

//    public Result<List<String>> _getShorts(String userId) {
//        return super.toJavaResult(
//                target
//                        .path(userId)
//                        .path(RestShorts.SHORTS)
//                        .request()
//                        .accept( MediaType.APPLICATION_JSON)
//                        .fakeSecurityContext(), new GenericType<List<String>>() {});
//    }
//
//    public Result<Void> _follow(String userId1, String userId2, boolean isFollowing) {
//        return super.toJavaResult(
//                target
//                        .path(userId1)
//                        .path(userId2)
//                        .path(RestShorts.FOLLOWERS)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId1, Token.Role.ADMIN))
//                        .post( Entity.entity(isFollowing, MediaType.APPLICATION_JSON)));
//    }
//
//    public Result<List<String>> _followers(String userId) {
//        return super.toJavaResult(
//                target
//                        .path(userId)
//                        .path(RestShorts.FOLLOWERS)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .accept( MediaType.APPLICATION_JSON)
//                        .fakeSecurityContext(), new GenericType<List<String>>() {});
//    }
//
//    public Result<Void> _like(String shortId, String userId, boolean isLiked) {
//        return super.toJavaResult(
//                target
//                        .path(shortId)
//                        .path(userId)
//                        .path(RestShorts.LIKES)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .post( Entity.entity(isLiked, MediaType.APPLICATION_JSON)));
//    }
//
//    public Result<List<String>> _likes(String shortId) {
//        return super.toJavaResult(
//                target
//                        .path(shortId)
//                        .path(RestShorts.LIKES)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .accept( MediaType.APPLICATION_JSON)
//                        .fakeSecurityContext(), new GenericType<List<String>>() {});
//    }
//
//    public Result<List<String>> _getFeed(String userId) {
//        return super.toJavaResult(
//                target
//                        .path(userId)
//                        .path(RestShorts.FEED)
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .accept( MediaType.APPLICATION_JSON)
//                        .fakeSecurityContext(), new GenericType<List<String>>() {});
//    }
//
//    public Result<Void> _deleteAllShorts(String userId, String token) {
//        return super.toJavaResult(
//                target
//                        .path(userId)
//                        .path(RestShorts.SHORTS)
//                        .queryParam(RestShorts.TOKEN, token )
//                        .request()
//                        .cookie("AUTH", Token.fakeSecurityContext(Token.Service.AUTH, userId, Token.Role.ADMIN))
//                        .delete());
//    }
//
//    public Result<Void> _verifyBlobURI(String blobId) {
//        return super.toJavaResult(
//                target
//                        .path(blobId)
//                        .request()
//                        .fakeSecurityContext());
//    }

    public Result<Void> _updateShortViews(String shortId, Long views) {
        return super.toJavaResult(
                target
                        .path(shortId).path(RestShorts.VIEWS)
                        .request()
                        .cookie("AUTH",Token.get(Token.Service.AUTH, shortId, Token.Role.USER))
                        .put( Entity.entity(views, MediaType.APPLICATION_JSON)));
    }

    @Override
    public Result<Short> createShort(SecurityContext sc, String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> deleteShort(SecurityContext sc, String shortId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return super.reTry( () -> _getShort(shortId));
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> follow(SecurityContext sc, String userId1, String userId2, boolean isFollowing) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<List<String>> followers(SecurityContext sc, String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> like(SecurityContext sc, String shortId, String userId, boolean isLiked) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<List<String>> likes(SecurityContext sc, String shortId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<List<String>> getFeed(SecurityContext sc, String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> deleteAllShorts(SecurityContext sc, String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Result<Void> updateShortViews(String shortId, Long views) {
        return super.reTry( () -> _updateShortViews(shortId, views));
    }
}