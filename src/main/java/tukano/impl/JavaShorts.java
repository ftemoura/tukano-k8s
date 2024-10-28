package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.errorOrVoid;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static utils.DB.getOne;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.ws.rs.core.SecurityContext;
import tukano.api.*;
import tukano.api.Short;
import tukano.impl.cache.RedisCacheShorts;
import tukano.impl.cache.ShortsCache;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.database.PostegreShorts;
import tukano.impl.database.ShortsDatabse;
import tukano.impl.rest.MainApplication;
import utils.FakeSecurityContext;

public class JavaShorts implements Shorts {

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	private ShortsCache cache;

	private ShortsDatabse dbImpl;
	private static Shorts instance;
	
	synchronized public static Shorts getInstance() {
		if( instance == null )
			instance = new JavaShorts();
		return instance;
	}
	
	private JavaShorts() {
		this.cache = new RedisCacheShorts();
		this.dbImpl = new PostegreShorts();
	}
	
	@Override
	public Result<Short> createShort(SecurityContext sc, String userId) {
		Log.info(() -> format("createShort : userId = %s\n", userId));

		Result<Short> result = errorOrResult( okUser(userId, sc), user -> {
			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", MainApplication.serverURI, Blobs.NAME, shortId);
			var shrt = new Short(shortId, userId, blobUrl);

			return errorOrValue(dbImpl.createShort(shrt), s -> s.copyWithLikes_And_Token(0));
		});

		if(result.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				Short shortResult = result.value();
				this.cache.cacheShort(shortResult, shortResult.getLastModified());
				this.cache.invalidateFeed(userId);
			}).start();
		}
		return result;
	}

	@Override
	public Result<Short> getShort(String shortId) {
		Log.info(() -> format("getShort : shortId = %s\n", shortId));

		if( shortId == null )
			return error(BAD_REQUEST);

		Result<Short> caheRes = this.cache.getShort(shortId);
		if(caheRes.isOK())
			return caheRes;
		var likes = dbImpl.getLikesCount(shortId);
		Result<Short> bdRes = errorOrValue(dbImpl.getShort(shortId), shrt -> shrt.copyWithLikes_And_Token( likes));
		if(bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				Short shrt = bdRes.value();
				this.cache.cacheShort(shrt, shrt.getLastModified());
			}).start();
		}
		return bdRes;
	}

	
	@Override
	public Result<Void> deleteShort(SecurityContext sc, String shortId) {
		Log.info(() -> format("deleteShort : shortId = %s\n", shortId));
		
		Result<Void> bdRes = errorOrResult( getShort(shortId), shrt -> {
			return errorOrResult( okUser( shrt.getOwnerId(), sc), user -> {
				return dbImpl.deleteShort(shrt);
			});
		});

		if (bdRes.isOK())
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.deleteShort(shortId);
			}).start();

		return bdRes;
	}

	@Override
	public Result<List<String>> getShorts(String userId) {
		Log.info(() -> format("getShorts : userId = %s\n", userId));

		Result<List<String>> cacheRes = this.cache.getShorts(userId);
		if(cacheRes.isOK())
			return cacheRes;

		Result<List<String>> bdRes = errorOrValue( okUser(userId), dbImpl.getShorts(userId));
		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.cacheShorts(userId, bdRes.value());
			}).start();
		}
		return bdRes;
	}

	@Override
	public Result<Void> follow(SecurityContext sc, String userId1, String userId2, boolean isFollowing) {
		Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s\n", userId1, userId2, isFollowing));

		Result<Void> bdRes = errorOrResult( okUser(userId1, sc), user -> {
			var f = new Following(userId1, userId2);
			return errorOrVoid( okUser( userId2), dbImpl.follow(f, isFollowing));
		});

		if(bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.invalidateFollowers(userId2);
				this.cache.invalidateFeed(userId1);
			}).start();
		}
		return bdRes;
	}

	@Override
	public Result<List<String>> followers(SecurityContext sc, String userId) {
		Log.info(() -> format("followers : userId = %s\n", userId));

		Result<List<String>> cacheRes = this.cache.getFollowers(userId);
		if (cacheRes.isOK())
			return cacheRes;

		Result<List<String>> bdRes = errorOrValue( okUser(userId, sc), dbImpl.followers(userId));

		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.cacheFollowers(userId, bdRes.value());
			}).start();
		}
		return bdRes;
	}

	@Override
	public Result<Void> like(SecurityContext sc, String shortId, String userId, boolean isLiked) {
		Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s\n", shortId, userId, isLiked));

		
		Result<Void> bdRes = errorOrResult( getShort(shortId), shrt -> {
			var l = new Likes(userId, shortId, shrt.getOwnerId());
			return errorOrVoid( okUser( userId, sc), dbImpl.like(l, isLiked));
		});

		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.invalidateLikes(shortId);
			}).start();
		}
		return bdRes;
	}

	@Override
	public Result<List<String>> likes(SecurityContext sc, String shortId) {
		Log.info(() -> format("likes : shortId = %s\n", shortId));

		Result<List<String>> cacheRes = this.cache.getLikes(shortId);
		if (cacheRes.isOK())
			return cacheRes;

		Result<List<String>> bdRes = errorOrResult( getShort(shortId), shrt -> {
			return errorOrValue( okUser( shrt.getOwnerId(), sc ), dbImpl.likes(shortId));
		});
		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.cacheLikes(shortId, bdRes.value());
			}).start();
		}
		return bdRes;
	}

	@Override
	public Result<List<String>> getFeed(SecurityContext sc, String userId) {
		Log.info(() -> format("getFeed : userId = %s\n", userId));
		Result<List<String>> cacheRes = this.cache.getFeed(userId);
		if (cacheRes.isOK())
			return cacheRes;

		Result<List<String>> bdRes = errorOrValue( okUser( userId, sc), dbImpl.getFeed(userId));

		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.cacheFeed(userId, bdRes.value());
			}).start();
		}
		return bdRes;
	}

	private Result<User> okUser(String userId, SecurityContext sc) {
		return JavaUsers.getInstance().getUser(sc, userId);
	}
	
	private Result<Void> okUser( String userId ) {
		var res = okUser(userId, FakeSecurityContext.get(userId));
		if( res.error() != FORBIDDEN )
			return ok();
		else
			return error( res.error() );
	}
	
	@Override
	public Result<Void> deleteAllShorts(String userId, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s, token = %s\n", userId, token));

		if( ! Token.isValid( token, Token.Service.INTERNAL, userId ) )
			return error(FORBIDDEN);

		return dbImpl.deleteAllShorts(userId, token);
	}
	
}