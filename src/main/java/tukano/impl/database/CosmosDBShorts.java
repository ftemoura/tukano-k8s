package tukano.impl.database;

import tukano.api.*;
import tukano.api.Short;
import tukano.impl.JavaBlobs;
import tukano.impl.Token;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.MainApplication;
import utils.ConfigLoader;

import java.util.List;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.*;

public class CosmosDBShorts extends CosmosDBLayer implements ShortsDatabse{
    private static Logger Log = Logger.getLogger(CosmosDBShorts.class.getName());
    private static final String SHORTS_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBShortsContainer();
    private static final String LIKES_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBLikesContainer();
    private static final String FOLLOWS_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBFollowsContainer();
    private static CosmosDBShorts instance;

    public CosmosDBShorts() {
        super();
    }

    public static synchronized CosmosDBLayer getInstance() {
        if (instance != null) return instance;
        instance = new CosmosDBShorts();
        return instance;
    }

    @Override
    public Result<Short> createShort(Short shrt) {
        return super.insertOne(shrt, SHORTS_CONTAINER_NAME);
    }

    @Override
    public Result<Void> deleteShort(Short shrt) {
        String shortId = shrt.getShortId();
        Result<ShortDAO> r = super.getOne(shortId, SHORTS_CONTAINER_NAME, ShortDAO.class);
        Result<Short> result = errorOrResult(r, shortDAO -> super.deleteOne(shortDAO, SHORTS_CONTAINER_NAME, shortDAO.get_etag()));
        if (!result.isOK()) return error(result.error());
        var blobUrl = format("%s/%s/%s", MainApplication.serverURI, Blobs.NAME, shortId);
        Result<Void> res = JavaBlobs.getInstance().delete(shrt.getShortId(), Token.get(Token.Service.BLOBS, blobUrl) );
        return res;
    }

    @Override
    public Long getLikesCount(String shortId) {
        List<String> likes = likes(shortId).value();
        return (long) likes.size();
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return super.getOne(shortId,SHORTS_CONTAINER_NAME, Short.class);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        String query = format("SELECT s.shortId FROM s WHERE s.ownerId = %s", userId);
        Result<List<Short>> shrts = super.query(query, SHORTS_CONTAINER_NAME, Short.class);
        if (!shrts.isOK()) return error(shrts.error());
        return ok(shrts.value().stream().map(Short::getShortId).toList());
    }

    @Override
    public Result<Following> follow(Following f, boolean isFollowing) {//TODO é preciso transação?
        if(isFollowing)
            return super.insertOne(f, FOLLOWS_CONTAINER_NAME);
        else
            return super.deleteOne(f, FOLLOWS_CONTAINER_NAME);
    }

    @Override
    public Result<List<String>> followers(String userId) {
        String query = format("SELECT %s.follower FROM %s WHERE %s.followee = %s",
                FOLLOWS_CONTAINER_NAME,FOLLOWS_CONTAINER_NAME,FOLLOWS_CONTAINER_NAME, userId);
        Result<List<Following>> followers = super.query(query, FOLLOWS_CONTAINER_NAME, Following.class);
        if(!followers.isOK()) return error(followers.error());
        return ok(followers.value().stream().map(Following::getFollower).toList());
    }

    @Override
    public Result<Likes> like(Likes l, boolean isLiked) {//TODO é preciso transação?
        if(isLiked)
            return super.insertOne(l, LIKES_CONTAINER_NAME);
        else
            return super.deleteOne(l, LIKES_CONTAINER_NAME);
    }

    @Override
    public Result<List<String>> likes(String shortId) {
        String query = format("SELECT likes.id FROM likes WHERE likes.shortId = '%s'", shortId);
        Result<List<Likes>> likes = super.query(query, LIKES_CONTAINER_NAME, Likes.class);
        if(!likes.isOK()) return error(likes.error());
        return ok(likes.value().stream().map(Likes::getUserId).toList());
    }

    @Override
    public Result<List<String>> getFeed(String userId) {
        return null;
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String token) {
        return null;
    }
}
