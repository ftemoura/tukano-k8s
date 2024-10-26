package tukano.impl.database;

import tukano.api.Result;
import tukano.api.UserImpl;
import tukano.api.User;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.Token;
import utils.DB;

import java.util.List;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

public class PostegreUsers implements UsersDatabase {

    @Override
    public Result<String> createUser(User user) {
        return errorOrValue( DB.insertOne(user), user.getUserId() );
    }

    @Override
    public Result<User> login(String userId, String pwd) {
        return validatedUserOrError( DB.getOne( userId, User.class), pwd);
    }

    @Override
    public Result<User> getUser(String userId) {
        return DB.getOne( userId, User.class);
    }

    @Override
    public Result<User> updateUser(String userId, UserImpl other) {
        return errorOrResult( DB.getOne( userId, UserImpl.class), user -> DB.updateOne( user.updateFrom(other)));
    }

    @Override
    public Result<User> deleteUser(String userId) {
        return DB.transaction( hibernate -> {
            errorOrResult( DB.getOne( userId, UserImpl.class), user -> {
                Result<User> userDelRes = DB.deleteOne(user);
                // Delete user shorts and related info asynchronously in a separate thread
                //Executors.defaultThreadFactory().newThread(() -> {
                JavaShorts.getInstance().deleteAllShorts(userId, /*Token.get(userId)*/ Token.get(Token.Service.INTERNAL, userId));
                JavaBlobs.getInstance().deleteAllBlobs(userId, /*Token.get(userId)*/ Token.get(Token.Service.INTERNAL, userId));
                //}).start();
                return userDelRes;
            });
        });
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        var query = format("SELECT * FROM User u WHERE UPPER(u.userId) LIKE '%%%s%%'", pattern.toUpperCase());
        return ok(DB.sql(query, UserImpl.class)
                .stream()
                .map(User::copyWithoutPassword)
                .toList());
    }

    private Result<User> validatedUserOrError(Result<User> res, String pwd ) {
        if (res.isOK())
            return res.value().pwd().equals(pwd) ? res : error(FORBIDDEN);
        else if (res.error() == NOT_FOUND)
            return error(FORBIDDEN);
        else
            return res;
    }

}
