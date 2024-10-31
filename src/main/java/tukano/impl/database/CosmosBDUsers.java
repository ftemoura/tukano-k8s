package tukano.impl.database;

import tukano.api.Result;
import tukano.api.User;
import tukano.api.UserDAO;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.Token;
import utils.ConfigLoader;
import utils.JSON;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

public class CosmosBDUsers extends CosmosDBLayer implements UsersDatabase{

    private static Logger Log = Logger.getLogger(CosmosBDUsers.class.getName());
    private static final String CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBUsersContainer();

    private static CosmosBDUsers instance;

    public CosmosBDUsers() {
        super();
    }

    public static synchronized CosmosDBLayer getInstance() {
        if (instance != null) return instance;
        instance = new CosmosBDUsers();
        return instance;
    }

    @Override
    public Result<User> createUser(User user) {
        return super.insertOne(user, CONTAINER_NAME);
    }

    @Override
    public Result<User> login(String userId, String pwd) {
        return validatedUserOrError(getUser(userId), pwd);
    }

    @Override
    public Result<User> getUser(String userId) {
         return super.getOne(userId,CONTAINER_NAME, User.class);
    }

    @Override
    public Result<User> updateUser(String userId, User other) {
        return errorOrResult( super.getOne( userId, CONTAINER_NAME, User.class), user -> super.updateOne( user.updateFrom(other), CONTAINER_NAME));
    }

    private Result<User> deleteUserWithoutRetry(String userId) {
        Result<UserDAO> r = super.getOne(userId, CONTAINER_NAME, UserDAO.class);
        Log.info(()-> format("deleteUser : %s\n", JSON.encode(r.value())));
        return errorOrResult(r, userDAO -> {
            Result<User> res = super.deleteOne(userDAO, CONTAINER_NAME, userDAO.get_etag());
            if(res.isOK()) {
                JavaShorts.getInstance().deleteAllShorts(userId,Token.get(Token.Service.INTERNAL, userId));
                JavaBlobs.getInstance().deleteAllBlobs(userId,Token.get(Token.Service.INTERNAL, userId));
            }
            return res;
        });
    }
    
    @Override
    public Result<User> deleteUser(String userId) {
        return super.retry(()->deleteUserWithoutRetry(userId), 3, 1000);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        var query = format("SELECT * FROM %s c WHERE CONTAINS(UPPER(c.id), '%s')", CONTAINER_NAME, pattern.toUpperCase());
        Result<List<User>> res = super.query(query, CONTAINER_NAME, User.class);
        if(!res.isOK()) return res;
        return ok(res.value()
                .stream()
                .map(User::copyWithoutPassword)
                .toList());
    }

    private Result<User> validatedUserOrError(Result<User> res, String pwd ) {
        if (res.isOK()) {
            Log.info(() -> format("validatedUserOrError : userId = %s, pwd = %s\n", res.value().pwd(), pwd));
            return res.value().pwd().equals(pwd) ? res : error(FORBIDDEN);
        }else if (res.error() == NOT_FOUND) {
            Log.info(() -> "NOt found");
            return error(FORBIDDEN);
        }else
            return res;
    }
}
