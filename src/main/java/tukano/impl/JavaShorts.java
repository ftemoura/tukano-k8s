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

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.User;
import tukano.impl.cache.RedisCacheShorts;
import tukano.impl.cache.ShortsCache;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.TukanoRestServer;
import utils.DB;
import utils.FakeSecurityContext;

public class JavaShorts implements Shorts {

	private static Logger Log = Logger.getLogger(JavaShorts.class.getName());

	private ShortsCache cache;
	private static Shorts instance;
	
	synchronized public static Shorts getInstance() {
		if( instance == null )
			instance = new JavaShorts();
		return instance;
	}
	
	private JavaShorts() {
		this.cache = new RedisCacheShorts();
	}
	
	@Override
	public Result<Short> createShort(SecurityContext sc, String userId) {
		Log.info(() -> format("createShort : userId = %s\n", userId));

		Result<Short> result = errorOrResult( okUser(userId, sc), user -> {
			var shortId = format("%s+%s", userId, UUID.randomUUID());
			var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId); 
			var shrt = new Short(shortId, userId, blobUrl);

			return errorOrValue(DB.insertOne(shrt), s -> s.copyWithLikes_And_Token(0));
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
		var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
		var likes = DB.sql(query, Long.class);
		Result<Short> bdRes = errorOrValue( getOne(shortId, Short.class), shrt -> shrt.copyWithLikes_And_Token( likes.get(0)));
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
				return DB.transaction( hibernate -> {

					hibernate.remove( shrt);
					
					var query = format("DELETE Likes l WHERE l.shortId = '%s'", shortId);
					hibernate.createNativeQuery( query, Likes.class).executeUpdate();
					JavaBlobs.getInstance().delete(shrt.getBlobUrl(), Token.get(Token.Service.BLOBS, shrt.getBlobUrl()) );
				});
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

		var query = format("SELECT s.shortId FROM Short s WHERE s.ownerId = '%s'", userId);
		Result<List<String>> bdRes = errorOrValue( okUser(userId), DB.sql( query, String.class));
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
			return errorOrVoid( okUser( userId2), isFollowing ? DB.insertOne( f ) : DB.deleteOne( f ));	
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

		var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);		
		Result<List<String>> bdRes = errorOrValue( okUser(userId, sc), DB.sql(query, String.class));

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
			return errorOrVoid( okUser( userId, sc), isLiked ? DB.insertOne( l ) : DB.deleteOne( l ));
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
			var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);
			return errorOrValue( okUser( shrt.getOwnerId(), sc ), DB.sql(query, String.class));
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

		final var QUERY_FMT = """
				SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'				
				UNION			
				SELECT s.shortId, s.timestamp FROM Short s, Following f 
					WHERE 
						f.followee = s.ownerId AND f.follower = '%s' 
				ORDER BY s.timestamp DESC""";

		Result<List<String>> bdRes = errorOrValue( okUser( userId, sc), DB.sql( format(QUERY_FMT, userId, userId), String.class));

		if (bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				this.cache.cacheFeed(userId, bdRes.value());
			}).start();
		}
		return bdRes;
	}
		
	protected Result<User> okUser( String userId, String pwd) {
		return JavaUsers.getInstance().getUser(FakeSecurityContext.get(userId), userId);
	}

	private Result<User> okUser( String userId, SecurityContext sc) {
		return JavaUsers.getInstance().getUser(sc, userId);
	}
	
	private Result<Void> okUser( String userId ) {
		var res = okUser(userId, "");
		if( res.error() != FORBIDDEN )
			return ok();
		else
			return error( res.error() );
	}
	
	@Override
	public Result<Void> deleteAllShorts(String userId, String token) {
		Log.info(() -> format("deleteAllShorts : userId = %s, token = %s\n", userId, token));

		// TODO fix tokens
		//if( ! Token.isValid( token, "shorts",userId ) )
		//	return error(FORBIDDEN);
		/*Result<User> user = okUser(userId, sc);
		if(!user.isOK()) //TODO ele nao tinha esta verificaçao
			return error(user.error());*/

		return DB.transaction( (hibernate) -> {
						
			//delete shorts
			var query1 = format("DELETE Short s WHERE s.ownerId = '%s'", userId);		
			hibernate.createQuery(query1, Short.class).executeUpdate();
			
			//delete follows
			var query2 = format("DELETE Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);		
			hibernate.createQuery(query2, Following.class).executeUpdate();
			
			//delete likes
			var query3 = format("DELETE Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);		
			hibernate.createQuery(query3, Likes.class).executeUpdate();
			
		});
	}
	
}