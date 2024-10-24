package tukano.clients.rest;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.*;
import tukano.api.Result;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.Token;


public class RestUsersClient extends RestClient implements Users {

	public RestUsersClient( String serverURI ) {
		super( serverURI, RestUsers.PATH );
	}
		
	private Result<String> _createUser(User user) {
		return super.toJavaResult( 
			target.request()
			.accept(MediaType.APPLICATION_JSON)
			.post(Entity.entity(user, MediaType.APPLICATION_JSON)), String.class );
	}

	private Result<User> _getUser(String userId, SecurityContext sc) {
		NewCookie c = new NewCookie.Builder(Token.Service.AUTH.toString())
				.value(Token.get(Token.Service.AUTH, userId))
				.expiry(Date.from(Instant.now().plusMillis(Token.MAX_TOKEN_AGE)))
				.path("/")
				.build();
		return super.toJavaResult(
				target.path( userId ).request()
				.accept(MediaType.APPLICATION_JSON)
						.cookie(c.getName(), c.getValue())
				.get(), User.class);
		// TODO fix clients?
	}
	
	public Result<User> _updateUser(SecurityContext sc, String userId, User user) {

		NewCookie c = new NewCookie.Builder(Token.Service.AUTH.toString())
				.value(Token.get(Token.Service.AUTH, userId))
				.expiry(Date.from(Instant.now().plusMillis(Token.MAX_TOKEN_AGE)))
				.path("/")
				.build();
		return super.toJavaResult(
				target
				.path( userId )
				.queryParam(RestUsers.PWD)
				.request()
				.accept(MediaType.APPLICATION_JSON)
						.cookie(c.getName(), c.getValue())
						.put(Entity.entity(user, MediaType.APPLICATION_JSON)), User.class);
	}

	public Result<User> _deleteUser(SecurityContext sc, String userId) {
		String token = sc.getUserPrincipal().getName();
		return super.toJavaResult(
				target
				.path( userId )
				.request()
				.accept(MediaType.APPLICATION_JSON)
						.header("Authorization", "Bearer " + token)
				.delete(), User.class);
	}

	public Result<List<User>> _searchUsers(String pattern) {
		return super.toJavaResult(
				target
				.queryParam(RestUsers.QUERY, pattern)
				.request()
				.accept(MediaType.APPLICATION_JSON)
				.get(), new GenericType<List<User>>() {});
	}

	@Override
	public Result<String> createUser(User user) {
		return super.reTry( () -> _createUser(user));
	}

	@Override
	public Result<Response> login(String userId, String pwd) {
		return Result.error(Result.ErrorCode.NOT_IMPLEMENTED);
	}

	@Override
	public Result<User> getUser(SecurityContext sc, String userId) {
		return super.reTry( () -> _getUser(userId, sc));
	}

	@Override
	public Result<User> updateUser(SecurityContext sc, String userId, User user) {
		return super.reTry( () -> _updateUser(sc, userId, user));
	}

	@Override
	public Result<User> deleteUser(SecurityContext sc, String userId) {
		return super.reTry( () -> _deleteUser(sc, userId));
	}

	@Override
	public Result<List<User>> searchUsers(String pattern) {
		return super.reTry( () -> _searchUsers(pattern));
	}
}
