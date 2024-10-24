package tukano.clients.rest;

import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.rest.RestShorts;

public class RestShortsClient extends RestClient implements Shorts{

	public RestShortsClient(String serverURI) {
		super(serverURI, RestShorts.PATH);
	}

	public Result<Short> _createShort(SecurityContext sc, String userId) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(userId)
				.request()
				.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + userToken)
				.post( Entity.json(null)), Short.class);
	}

	public Result<Void> _deleteShort(SecurityContext sc, String shortId) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(shortId)
				.request()
						.header("Authorization", "Bearer " + userToken)
				.delete());
	}

	public Result<Short> _getShort(String shortId) {
		return super.toJavaResult(
				target
				.path(shortId)
				.request()
				.get(), Short.class);
	}

	public Result<List<String>> _getShorts(String userId) {
		return super.toJavaResult(
				target
				.path(userId)
				.path(RestShorts.SHORTS)
				.request()
				.accept( MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<String>>() {});
	}

	public Result<Void> _follow(SecurityContext sc, String userId1, String userId2, boolean isFollowing) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(userId1)
				.path(userId2)
				.path(RestShorts.FOLLOWERS)
				.request()
						.header("Authorization", "Bearer " + userToken)
				.post( Entity.entity(isFollowing, MediaType.APPLICATION_JSON)));
	}

	public Result<List<String>> _followers(SecurityContext sc, String userId) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(userId)
				.path(RestShorts.FOLLOWERS)
				.request()
				.accept( MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + userToken)
				.get(), new GenericType<List<String>>() {});
	}

	public Result<Void> _like(SecurityContext sc, String shortId, String userId, boolean isLiked) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(shortId)
				.path(userId)
				.path(RestShorts.LIKES)
				.request()
						.header("Authorization", "Bearer " + userToken)
				.post( Entity.entity(isLiked, MediaType.APPLICATION_JSON)));
	}

	public Result<List<String>> _likes(SecurityContext sc, String shortId) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(shortId)
				.path(RestShorts.LIKES)
				.request()
						.header("Authorization", "Bearer " + userToken)
				.accept( MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<String>>() {});
	}

	public Result<List<String>> _getFeed(SecurityContext sc, String userId) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(userId)
				.path(RestShorts.FEED)
				.request()
						.header("Authorization", "Bearer " + userToken)
				.accept( MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<String>>() {});
	}

	public Result<Void> _deleteAllShorts(SecurityContext sc, String userId, String token) {
		String userToken = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path(userId)
				.path(RestShorts.SHORTS)
				.queryParam(RestShorts.TOKEN, token )
				.request()
						.header("Authorization", "Bearer " + userToken)
				.delete());
	}
	
	public Result<Void> _verifyBlobURI(String blobId) {
		return super.toJavaResult(
				target
				.path(blobId)
				.request()
				.get());
	}
		
	@Override
	public Result<Short> createShort(SecurityContext sc, String userId) {
		return super.reTry( () -> _createShort(sc, userId));
	}

	@Override
	public Result<Void> deleteShort(SecurityContext sc, String shortId) {
		return super.reTry( () -> _deleteShort(sc, shortId));
	}

	@Override
	public Result<Short> getShort(String shortId) {
		return super.reTry( () -> _getShort(shortId));
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		return super.reTry( () -> _getShorts(userId));
	}

	@Override
	public Result<Void> follow(SecurityContext sc, String userId1, String userId2, boolean isFollowing) {
		return super.reTry( () -> _follow(sc, userId1, userId2, isFollowing));
	}

	@Override
	public Result<List<String>> followers(SecurityContext sc, String userId) {
		return super.reTry( () -> _followers(sc, userId));
	}

	@Override
	public Result<Void> like(SecurityContext sc, String shortId, String userId, boolean isLiked) {
		return super.reTry( () -> _like(sc, shortId, userId, isLiked));
	}

	@Override
	public Result<List<String>> likes(SecurityContext sc, String shortId) {
		return super.reTry( () -> _likes(sc, shortId));
	}

	@Override
	public Result<List<String>> getFeed(SecurityContext sc, String userId) {
		return super.reTry( () -> _getFeed(sc, userId));
	}

	@Override
	public Result<Void> deleteAllShorts(SecurityContext sc, String userId, String token) {
		return super.reTry( () -> _deleteAllShorts(sc, userId, token));
	}
}
