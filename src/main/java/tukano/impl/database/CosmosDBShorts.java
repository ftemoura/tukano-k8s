package tukano.impl.database;

import tukano.api.Result;
import tukano.api.Short;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import utils.ConfigLoader;

import java.util.List;
import java.util.logging.Logger;

public class CosmosDBShorts extends CosmosDBLayer implements ShortsDatabse{
    private static Logger Log = Logger.getLogger(CosmosDBShorts.class.getName());
    private static final String SHORTS_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBShortsContainer();
    private static final String LIKES_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBLikesContainer();
    private static final String FOLLOWS_CONTAINER_NAME = ConfigLoader.getInstance().getCosmosDBFollowsContainer();

    public CosmosDBShorts() {
        super();
    }

    @Override
    public Result<Short> createShort(Short shrt) {
        return null;
    }

    @Override
    public Result<Void> deleteShort(Short shrt) {
        return null;
    }

    @Override
    public Long getLikesCount(String shortId) {
        return null;
    }

    @Override
    public Result<Short> getShort(String shortId) {
        return null;
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        return null;
    }

    @Override
    public Result<Following> follow(Following f, boolean isFollowing) {
        return null;
    }

    @Override
    public Result<List<String>> followers(String userId) {
        return null;
    }

    @Override
    public Result<Likes> like(Likes l, boolean isLiked) {
        return null;
    }

    @Override
    public Result<List<String>> likes(String shortId) {
        return null;
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
