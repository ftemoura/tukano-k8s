package tukano.impl.cache;

import tukano.api.Result;
import tukano.api.User;

public interface UsersCache {
    void cacheUser(User user);
    void deleteUser(String userId);
    Result<User> getUser(String userId);
}
