package tukano.impl.database;

import tukano.api.*;
import tukano.api.Short;
import tukano.impl.JavaBlobs;
import tukano.impl.Token;
import tukano.impl.data.Following;
import tukano.impl.data.FollowingDAO;
import tukano.impl.data.Likes;
import tukano.impl.data.LikesDAO;
import tukano.impl.rest.MainApplication;
import utils.ConfigLoader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.INTERNAL_ERROR;

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


    private Result<Void> deleteShortWithoutRetry(Short shrt){
        String shortId = shrt.getShortId();
        Result<ShortDAO> r = super.getOne(shortId, SHORTS_CONTAINER_NAME, ShortDAO.class);
        Result<Short> result = errorOrResult(r, shortDAO -> super.deleteOne(shortDAO, SHORTS_CONTAINER_NAME, shortDAO.get_etag()));
        if (!result.isOK()) return error(result.error());
        //var blobUrl = format("%s/%s/%s", MainApplication.serverURI, Blobs.NAME, shortId);
        JavaBlobs.getInstance().delete(shrt.getShortId(), Token.get(Token.Service.BLOBS, shortId) );
        if (!result.isOK()) return error(result.error());
        return ok();
    }
    @Override
    public Result<Void> deleteShort(Short shrt) {
        return super.retry(()->deleteShortWithoutRetry(shrt), 3, 1000);
    }

    @Override
    public Long getLikesCount(String shortId) {
        Result<List<String>> likesRes = likes(shortId);
        if (!likesRes.isOK()) return 0L;
        return (long) likesRes.value().size();
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return super.getOne(shortId,SHORTS_CONTAINER_NAME, Short.class);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        String query = format("SELECT c.id FROM %s c WHERE c.ownerId = '%s'", SHORTS_CONTAINER_NAME, userId);
        Result<List<Short>> shrts = super.query(query, SHORTS_CONTAINER_NAME, Short.class);
        if (!shrts.isOK()) return error(shrts.error());
        return ok(shrts.value().stream().map(Short::getShortId).toList());
    }

    @Override
    public Result<Following> follow(Following f, boolean isFollowing) {
        FollowingDAO followingDAO = new FollowingDAO(f);
        return isFollowing? super.insertOne(followingDAO, FOLLOWS_CONTAINER_NAME) : super.deleteOne(followingDAO, FOLLOWS_CONTAINER_NAME);
    }

    @Override
    public Result<List<String>> followers(String userId) {
        String query = format("SELECT c.follower FROM %s c WHERE c.followee = '%s'", FOLLOWS_CONTAINER_NAME, userId);
        Result<List<Following>> followers = super.query(query, FOLLOWS_CONTAINER_NAME, Following.class);
        if(!followers.isOK()) return error(followers.error());
        return ok(followers.value().stream().map(Following::getFollower).toList());
    }

    @Override
    public Result<Likes> like(Likes l, boolean isLiked) {
        LikesDAO likesDAO = new LikesDAO(l);
        return isLiked ? super.insertOne(likesDAO, LIKES_CONTAINER_NAME) : super.deleteOne(likesDAO, LIKES_CONTAINER_NAME);
    }

    @Override
    public Result<List<String>> likes(String shortId) {
        String query = format("SELECT c.userId FROM %s c WHERE c.shortId = '%s'", LIKES_CONTAINER_NAME, shortId);
        Result<List<Likes>> likes = super.query(query, LIKES_CONTAINER_NAME, Likes.class);
        if(!likes.isOK()) return error(likes.error());
        return ok(likes.value().stream().map(Likes::getUserId).toList());
    }

    @Override
    public Result<List<String>> getFeed(String userId) {
        String query = format("SELECT c.id FROM %s c WHERE c.follower = '%s'", FOLLOWS_CONTAINER_NAME, userId);
        Result<List<Following>> follows = super.query(query, FOLLOWS_CONTAINER_NAME, Following.class);
        if(!follows.isOK()) return error(follows.error());

        String inBody = follows.value().stream()
                .map(id -> "\"" + id + "\"")
                .collect(Collectors.joining(", "));
        if (inBody.isEmpty())
            inBody = "\"" + userId + "\"";
        else
            inBody = inBody + ", \"" + userId + "\"";
        System.out.println(inBody);
        String query2 = format("SELECT c.id, c.timestamp FROM %s c WHERE c.ownerId IN (%s) ORDER BY c.timestamp DESC", SHORTS_CONTAINER_NAME, inBody);
        Result<List<Short>> shrts = super.query(query2, SHORTS_CONTAINER_NAME, Short.class);
        if(!shrts.isOK()) return error(shrts.error());
        return ok(shrts.value().stream().map(Short::getShortId).toList());
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String token) {

        String query1 = format("SELECT c.id FROM %s c WHERE c.ownerId = '%s'", SHORTS_CONTAINER_NAME, userId);
        Result<List<Short>> shortsToDeleteRes = super.query(query1, SHORTS_CONTAINER_NAME, Short.class);
        if (!shortsToDeleteRes.isOK()) return error(shortsToDeleteRes.error());

        String query2 = format("SELECT c.id FROM %s c WHERE c.follower = '%s' OR c.followee = '%s'", FOLLOWS_CONTAINER_NAME, userId, userId);
        Result<List<FollowingDAO>> followsToDeleteRes = super.query(query2, FOLLOWS_CONTAINER_NAME, FollowingDAO.class);
        if (!followsToDeleteRes.isOK()) return error(followsToDeleteRes.error());

        String query3 = format("SELECT c.id FROM %s c WHERE c.ownerId = '%s' OR c.userId = '%s'", LIKES_CONTAINER_NAME, userId, userId);
        Result<List<LikesDAO>> likesToDeleteRes = super.query(query3, LIKES_CONTAINER_NAME, LikesDAO.class);
        if (!likesToDeleteRes.isOK()) return error(likesToDeleteRes.error());

        List<Short> shortsToDelete = shortsToDeleteRes.value();
        List<FollowingDAO> followsToDelete = followsToDeleteRes.value();
        List<LikesDAO> likesToDelete = likesToDeleteRes.value();

        int maxRetries = 10;
        long retryDelay = 1000;
        for (Short shortItem : shortsToDelete)
            deleteWithRetry(shortItem, SHORTS_CONTAINER_NAME, maxRetries, retryDelay);

        for (Following follow : followsToDelete) {
            deleteWithRetry(follow, FOLLOWS_CONTAINER_NAME, maxRetries, retryDelay);
        }

        for (Likes like : likesToDelete) {
            deleteWithRetry(like, LIKES_CONTAINER_NAME, maxRetries, retryDelay);
        }

        return ok();
    }

    @Override
    public Result<Void> updateShortViews(String shortId, Long views) {
        Result<ShortDAO> shrt = super.getOne(shortId, SHORTS_CONTAINER_NAME, ShortDAO.class);
        if (!shrt.isOK()) return error(shrt.error());
        ShortDAO shortDAO = shrt.value();
        shortDAO.setViews(views);
        Result<ShortDAO> res = super.updateOne(shortDAO, SHORTS_CONTAINER_NAME, shortDAO.get_etag());
        if (!res.isOK()) return error(res.error());
        return ok();
    }

    private boolean shouldRetry(Result<?> res) {
        return !res.isOK() && res.error() == INTERNAL_ERROR;
    }
    private <T> void deleteWithRetry(T item, String containerName, int maxRetries, long retryDelay) {
        int attempt = 0;
        boolean success = false;

        while (attempt < maxRetries && !success) {
            Result<T> res = super.deleteOne(item, containerName);
            if (shouldRetry(res)) {
                attempt++;
                if (attempt >= maxRetries) {
                    Log.info("Failed to delete item in container: " + containerName + " after " + maxRetries + " attempts.");
                } else {
                    Log.info("Retry " + attempt + " for item deletion in container: " + containerName);
                }
            }else {
                success = true;
            }
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Log.info("Retry delay interrupted: " + ie.getMessage());
                break;
            }
        }
    }
}
