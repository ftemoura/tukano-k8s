package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.ws.rs.core.*;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.impl.cache.RedisCacheUsers;
import tukano.impl.cache.UsersCache;
import tukano.impl.database.*;
import utils.ConfigLoader;

public class JavaUsers implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private UsersCache cache;

	private UsersDatabase dbImpl;

	private static Users instance;

	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}
	
	private JavaUsers() {
		this.cache = new RedisCacheUsers();

		if (ConfigLoader.getInstance().getUsedDbType().equals(DbType.COSMOS.toString()))
			this.dbImpl = new CosmosBDUsers();
		else if (ConfigLoader.getInstance().getUsedDbType().equals(DbType.POSTGRESQL.toString()))
			this.dbImpl = new PostegreUsers();
		else Log.info(() -> format("Invalid DB Type"));
	}
	
	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo(user) )
				return error(BAD_REQUEST);

		Result<User> bdRes = dbImpl.createUser(user);
		if(bdRes.isOK()) {
			Executors.defaultThreadFactory().newThread(() -> {
				cache.cacheUser(bdRes.value());
			}).start();
		}
		return errorOrValue(bdRes, user.getUserId());
	}

	@Override
	public Result<Response> login(String userId, String pwd) {
		if (userId == null)
			return error(BAD_REQUEST);
		Result<User> res = dbImpl.login(userId, pwd);
		if (!res.isOK()) {
			return Result.error(res.error());
		} else {
			NewCookie c = new NewCookie.Builder(Token.Service.AUTH.toString())
					.value(Token.get(Token.Service.AUTH, userId, Token.Role.USER))
					.expiry(Date.from(Instant.now().plusMillis(Token.MAX_TOKEN_AGE)))
					.path("/")
					.build();
			return ok(Response.ok().cookie(c).build());
		}
	}

	@Override
	public Result<User> getUser(SecurityContext sc, String userId) {
		Log.info( () -> format("getUser : userId = %s\n", userId));

		if (userId == null)
			return error(BAD_REQUEST);

		// authorization: userId requested matches the requester token
		if( !userId.equals(sc.getUserPrincipal().getName()))
			return error(FORBIDDEN);

		Result<User> cacheRes = cache.getUser(userId);
		if (cacheRes.isOK())
			return cacheRes;
		Result<User> bdRes = dbImpl.getUser(userId);

		if(bdRes.isOK())
			Executors.defaultThreadFactory().newThread(() -> {
				cache.cacheUser( bdRes.value());
			}).start();
		return bdRes;
	}

	@Override
	public Result<User> updateUser(SecurityContext sc, String userId, User other) {
		Log.info(() -> format("updateUser : userId = %s, user: %s\n", userId, other));

		if (badUpdateUserInfo(userId, other))
			return error(BAD_REQUEST);

		// authorization: userId requested matches the requester token
		if( !userId.equals(sc.getUserPrincipal().getName()))
			return error(FORBIDDEN);

		Result<User> bdRes = dbImpl.updateUser(userId, other);

		if(bdRes.isOK())
			Executors.defaultThreadFactory().newThread(() -> {
				cache.cacheUser( bdRes.value());
			}).start();
		return bdRes;
	}

	@Override
	public Result<User> deleteUser(SecurityContext sc, String userId) {
		Log.info(() -> format("deleteUser : userId = %s\n", userId));

		if (userId == null || sc == null )
			return error(BAD_REQUEST);

		// authorization: userId requested matches the requester token
		if( !userId.equals(sc.getUserPrincipal().getName()))
			return error(FORBIDDEN);

		Result<User> dbRes = dbImpl.deleteUser(userId);

		if(dbRes.isOK())
			Executors.defaultThreadFactory().newThread(() -> {
				//cache.deleteUser(userId);
				cache.invalidateAllUserInfo(userId);
			}).start();
		return dbRes;
	}

	@Override//TODO compensa cache?
	public Result<List<User>> searchUsers(String pattern) {
		Log.info( () -> format("searchUsers : patterns = %s\n", pattern));

		return dbImpl.searchUsers(pattern);
	}


	private boolean badUserInfo( User user) {
		return (user.getUserId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}
	
	private boolean badUpdateUserInfo( String userId, User info) {
		return (userId == null ||  info.getUserId() != null && ! userId.equals( info.getUserId()));
	}
}
