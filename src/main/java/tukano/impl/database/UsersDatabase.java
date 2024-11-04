package tukano.impl.database;

import tukano.api.Result;
import tukano.api.User;

import java.util.List;

public interface UsersDatabase {
    Result<User> createUser(User user);
    Result<User> login(String userId, String pwd);
    Result<User> getUser(String userId);
    Result<User> updateUser(String userId, User other);
    Result<User> deleteUser(String userId);
    Result<List<User>> searchUsers(String pattern);
}
