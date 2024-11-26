package tukano.impl.database;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import tukano.api.clients.RestBlobsClient;
import tukano.impl.JavaBlobs;
import tukano.impl.Token;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.MainApplication;
import utils.DB;

import java.util.List;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static utils.DB.getOne;

public class PostegreShorts implements ShortsDatabse{
    private static RestBlobsClient blobs;
    public PostegreShorts() {
        blobs = new RestBlobsClient(MainApplication.serverURI);
    }
    @Override
    public Result<Short> createShort(Short shrt) {
        return DB.insertOne(shrt);
    }

    @Override
    public Result<Void> deleteShort(Short shrt, User deletedBy) {
        String shortId = shrt.getShortId();
        return DB.transaction( hibernate -> {
            hibernate.remove( shrt);
            var query = format("DELETE FROM \"Likes\" l WHERE \"shortId\" = '%s'", shortId);
            hibernate.createNativeQuery( query, Likes.class).executeUpdate();
            //var blobUrl = format("%s/%s/%s", MainApplication.serverURI, Blobs.NAME, shortId);
            blobs.delete(shrt.getShortId(), Token.get(Token.Service.BLOBS, shortId, deletedBy.getRole()) );
        });
    }

    @Override
    public Long getLikesCount(String shortId) {
        var query = format("SELECT count(*) FROM \"Likes\" l WHERE \"shortId\" = '%s'", shortId);
        List<Long> res = DB.sql(query, Long.class);
        return res.get(0);
    }
    @Override
    public Result<Short> getShort(String shortId) {
        return getOne(shortId, Short.class);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        var query = format("SELECT \"shortId\" FROM \"Short\" s WHERE \"ownerId\" = '%s'", userId);
        return ok(DB.sql( query, String.class));
    }

    @Override
    public Result<Following> follow(Following f, boolean isFollowing) {
        return isFollowing ? DB.insertOne( f ) : DB.deleteOne( f );
    }

    @Override
    public Result<List<String>> followers(String userId) {
        var query = format("SELECT \"follower\" FROM \"Following\" f WHERE \"followee\" = '%s'", userId);
        return ok(DB.sql(query, String.class));
    }

    @Override
    public Result<Likes> like(Likes l, boolean isLiked) {
        return isLiked ? DB.insertOne( l ) : DB.deleteOne( l );
    }

    @Override
    public Result<List<String>> likes(String shortId) {
        var query = format("SELECT \"userId\" FROM \"Likes\" l WHERE \"shortId\" = '%s'", shortId);
        return ok(DB.sql(query, String.class));
    }

    @Override
    public Result<List<String>> getFeed(String userId) {
        final var QUERY_FMT = """
				SELECT \"shortId\", \"timestamp\" FROM "Short" s WHERE	\"ownerId\" = '%s'				
				UNION			
				SELECT \"shortId\", \"timestamp\" FROM "Short" s, "Following" f 
					WHERE 
						\"followee\" = \"ownerId\" AND \"follower\" = '%s' 
				ORDER BY \"timestamp\" DESC""";
        return  ok(DB.sql( format(QUERY_FMT, userId, userId), String.class));
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String token) {

        if(Token.isValid(token, Token.Service.BLOBS, userId, Token.Role.ADMIN))
            blobs.deleteAllBlobs(userId, token);
        return DB.transaction( (hibernate) -> {

            //delete shorts
            var query1 = format("DELETE FROM \"Short\" s WHERE \"ownerId\" = '%s'", userId);
            hibernate.createNativeQuery(query1, Short.class).executeUpdate();

            //delete follows
            var query2 = format("DELETE FROM \"Following\" f WHERE \"follower\" = '%s' OR \"followee\" = '%s'", userId, userId);
            hibernate.createNativeQuery(query2, Following.class).executeUpdate();

            //delete likes
            var query3 = format("DELETE FROM \"Likes\" l WHERE \"ownerId\" = '%s' OR \"userId\" = '%s'", userId, userId);
            hibernate.createNativeQuery(query3, Likes.class).executeUpdate();

        });
    }

    @Override
    public Result<Void> updateShortViews(String shortId, Long views) {
        Result<Short> res = errorOrResult( DB.getOne( shortId, Short.class), shrt -> {
            shrt.setViews(views);
            return  DB.updateOne(shrt);
        });
        if(!res.isOK()) return error(res.error());
        return ok();
    }
}
