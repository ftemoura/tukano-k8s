package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.errorOrResult;
import static tukano.api.Result.errorOrValue;
import static tukano.api.Result.ok;
import static tukano.api.Result.ErrorCode.BAD_REQUEST;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.time.Instant;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import jakarta.ws.rs.core.*;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import utils.DB;

public class JavaUsers implements Users {

	private static Logger Log = Logger.getLogger(JavaUsers.class.getName());

	private static Users instance;

	synchronized public static Users getInstance() {
		if( instance == null )
			instance = new JavaUsers();
		return instance;
	}
	
	private JavaUsers() {}
	
	@Override
	public Result<String> createUser(User user) {
		Log.info(() -> format("createUser : %s\n", user));

		if( badUserInfo( user ) )
				return error(BAD_REQUEST);

		return errorOrValue( DB.insertOne( user), user.getUserId() );
	}

	@Override
	public Result<Response> login(String userId, String pwd) {
		if (userId == null)
			return error(BAD_REQUEST);
		var res = validatedUserOrError( DB.getOne( userId, User.class), pwd);
		if (!res.isOK()) {
			return Result.error(res.error());
		} else {
			NewCookie c = new NewCookie.Builder(Token.Service.AUTH.toString())
					.value(Token.get(Token.Service.AUTH, userId))
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

		return  DB.getOne( userId, User.class);
	}

	@Override
	public Result<User> updateUser(String userId, String pwd, User other) {
		Log.info(() -> format("updateUser : userId = %s, pwd = %s, user: %s\n", userId, pwd, other));

		if (badUpdateUserInfo(userId, pwd, other))
			return error(BAD_REQUEST);

		return errorOrResult( validatedUserOrError(DB.getOne( userId, User.class), pwd), user -> DB.updateOne( user.updateFrom(other)));
	}

	@Override
	public Result<User> deleteUser(String userId, String pwd) {
		Log.info(() -> format("deleteUser : userId = %s, pwd = %s\n", userId, pwd));

		if (userId == null || pwd == null )
			return error(BAD_REQUEST);

		return errorOrResult( validatedUserOrError(DB.getOne( userId, User.class), pwd), user -> {

			// Delete user shorts and related info asynchronously in a separate thread
			Executors.defaultThreadFactory().newThread( () -> {
				//TODO fix tokens
				JavaShorts.getInstance().deleteAllShorts(userId, pwd, /*Token.get(userId)*/ Token.get(Token.Service.BLOBS, userId));
				JavaBlobs.getInstance().deleteAllBlobs(userId, /*Token.get(userId)*/ Token.get(Token.Service.BLOBS, userId));
			}).start();
			
			return DB.deleteOne( user);
		});
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		Log.info( () -> format("searchUsers : patterns = %s\n", pattern));

		var query = format("SELECT * FROM User u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
		var hits = DB.sql(query, User.class)
				.stream()
				.map(User::copyWithoutPassword)
				.toList();

		return ok(hits);
	}

	
	private Result<User> validatedUserOrError( Result<User> res, String pwd ) {
		if( res.isOK())
			return res.value().getPwd().equals( pwd ) ? res : error(FORBIDDEN);
		else
			return res;
	}
	
	private boolean badUserInfo( User user) {
		return (user.userId() == null || user.pwd() == null || user.displayName() == null || user.email() == null);
	}
	
	private boolean badUpdateUserInfo( String userId, String pwd, User info) {
		return (userId == null || pwd == null || info.getUserId() != null && ! userId.equals( info.getUserId()));
	}
}
