package tukano.impl.database;

import tukano.api.Result;
import tukano.api.UserImpl;
import tukano.api.UserImplDAO;
import tukano.api.User;
import utils.ConfigLoader;

import java.util.List;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.FORBIDDEN;
import static tukano.api.Result.ErrorCode.NOT_FOUND;

public class CosmosBDUsers extends CosmosDBLayer implements UsersDatabase{

    private static final String CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBUsersContainer();

    private static CosmosBDUsers instance;

    public CosmosBDUsers() {
        super(CONTAINER_NAME);
    }

    public static synchronized CosmosDBLayer getInstance() {
        if (instance != null) return instance;
        instance = new CosmosBDUsers();
        return instance;
    }

    @Override
    public Result<String> createUser(User user) {
        return errorOrValue(super.insertOne(user), user.getUserId());
    }

    @Override
    public Result<User> login(String userId, String pwd) {
        return validatedUserOrError(getUser(userId), pwd);
    }

    @Override
    public Result<User> getUser(String userId) {
        Result<UserImplDAO> res = super.getOne(userId, UserImplDAO.class);

        return null;
    }

    @Override
    public Result<User> updateUser(String userId, UserImpl other) {
        return errorOrResult( getUser(userId), user ->{
            //Result<UserImplDAO> res = super.updateOne(user.updateFrom(other));
           // return convertDAOToUser(res);
            return null;
        });
    }

    @Override
    public Result<User> deleteUser(String userId) {
        return null;
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        return null;
//        var query = format("SELECT * FROM c WHERE CONTAINS(UPPER(c.userId), '%s')", pattern.toUpperCase());
//        Result<List<User>> queryRes = super.query(UserImpl.class, query);
//        if(!queryRes.isOK()) return queryRes;
//        return ok(queryRes.value()
//                .stream()
//                .map(User::copyWithoutPassword)
//                .toList());
    }

    private Result<User> convertDAOToUser(Result<UserImplDAO> res) {
        if(!res.isOK()) return error(res.error());
        UserImplDAO userDAO = res.value();
        User user = res.value();
        //user.setLastModified(userDAO.g);
        return ok(user);
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
