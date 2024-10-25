package tukano.impl.rest;

import java.util.List;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.rest.RestShorts;
import tukano.impl.JavaShorts;

@Singleton
public class RestShortsResource extends RestResource implements RestShorts {

	static final Shorts impl = JavaShorts.getInstance();
		
	@Override
	public Short createShort(SecurityContext sc, String userId) {
		return super.resultOrThrow( impl.createShort(sc, userId));
	}

	@Override
	public void deleteShort(SecurityContext sc, String shortId) {
		super.resultOrThrow( impl.deleteShort(sc, shortId));
	}

	@Override
	public Short getShort(String shortId) {
		return super.resultOrThrow( impl.getShort(shortId));
	}
	@Override
	public List<String> getShorts(String userId) {
		return super.resultOrThrow( impl.getShorts(userId));
	}

	@Override
	public void follow(SecurityContext sc, String userId1, String userId2, boolean isFollowing) {
		super.resultOrThrow( impl.follow(sc, userId1, userId2, isFollowing));
	}

	@Override
	public List<String> followers(SecurityContext sc, String userId) {
		return super.resultOrThrow( impl.followers(sc, userId));
	}

	@Override
	public void like(SecurityContext sc, String shortId, String userId, boolean isLiked) {
		super.resultOrThrow( impl.like(sc, shortId, userId, isLiked));
	}

	@Override
	public List<String> likes(SecurityContext sc, String shortId) {
		return super.resultOrThrow( impl.likes(sc, shortId));
	}

	@Override
	public List<String> getFeed(SecurityContext sc, String userId) {
		return super.resultOrThrow( impl.getFeed(sc, userId));
	}

	@Override
	public void deleteAllShorts(String userId, String token) {
		super.resultOrThrow( impl.deleteAllShorts(userId, token));
	}	
}
