package tukano.impl.rest;

import java.util.List;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.UserImpl;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.JavaUsers;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

	final Users impl;
	public RestUsersResource() {
		this.impl = JavaUsers.getInstance();
	}
	
	@Override
	public String createUser(User user) {
		return super.resultOrThrow( impl.createUser(user));
	}

	@Override
	public Response login(String userId, String pwd) {
		return super.resultOrThrow(impl.login(userId, pwd));
	}

	@Override
	public User getUser(SecurityContext sc, String name) {
		return super.resultOrThrow( impl.getUser(sc, name));
	}
	
	@Override
	public User updateUser(SecurityContext sc, String name, UserImpl userImpl) {
		return super.resultOrThrow( impl.updateUser(sc, name, userImpl));
	}

	@Override
	public User deleteUser(SecurityContext sc, String name) {
		return super.resultOrThrow( impl.deleteUser(sc, name));
	}

	@Override
	public List<User> searchUsers(String pattern) {
		return super.resultOrThrow( impl.searchUsers( pattern));
	}
}
