package tukano.impl.database;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Shorts;
import tukano.api.User;
import tukano.api.clients.RestBlobsClient;
import tukano.impl.JavaShorts;
import tukano.impl.Token;
import utils.DB;
import utils.Auth;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

public class PostegreUsers implements UsersDatabase {
    private static Logger Log = Logger.getLogger(PostegreUsers.class.getName());
    private Blobs blobs;
    private Shorts shorts;

    public PostegreUsers() {
        this.blobs = new RestBlobsClient();
        this.shorts = JavaShorts.getInstance();
    }
    @Override
    public Result<User> createUser(User user) {
        return DB.insertOne(user);
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
    public Result<User> updateUser(String userId, User other) {
        return errorOrResult( DB.getOne( userId, User.class), user -> DB.updateOne( user.updateFrom(other)));
    }

    @Override
    public Result<User> deleteUser(String userId) {
        return DB.transaction( hibernate -> {
            return errorOrResult( DB.getOne( userId, User.class), user -> {
                Result<User> userDelRes = DB.deleteOne(user);
                shorts.deleteAllShorts(
                        Auth.fakeSecurityContext(Token.get(Token.Service.AUTH, user.getUserId(), user.getRole())),
                        userId);
                blobs.deleteAllBlobs(Auth.fakeSecurityContext(Token.get(Token.Service.AUTH, userId, user.getRole())), userId);
                return userDelRes;
            });
        });
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        var query = format("SELECT * FROM \"User\" WHERE UPPER(\"userId\") LIKE '%%%s%%'", pattern.toUpperCase());
        Log.info("Query: " + query);
        return ok(DB.sql(query, User.class)
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
