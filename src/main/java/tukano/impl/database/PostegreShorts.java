package tukano.impl.database;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.impl.JavaBlobs;
import tukano.impl.Token;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.rest.MainApplication;
import utils.DB;

import java.util.List;

import static java.lang.String.format;
import static tukano.api.Result.ok;
import static utils.DB.getOne;

public class PostegreShorts implements ShortsDatabse{
    @Override
    public Result<Short> createShort(Short shrt) {
        return DB.insertOne(shrt);
    }

    @Override
    public Result<Void> deleteShort(Short shrt) {
        String shortId = shrt.getShortId();
        return DB.transaction( hibernate -> {
            hibernate.remove( shrt);
            var query = format("DELETE Likes l WHERE l.shortId = '%s'", shortId);
            hibernate.createMutationQuery(query).executeUpdate();
            var blobUrl = format("%s/%s/%s", MainApplication.serverURI, Blobs.NAME, shortId);
            //TODO e se o delete falhar?
            JavaBlobs.getInstance().delete(shrt.getShortId(), Token.get(Token.Service.BLOBS, blobUrl) );
        });
    }

    @Override
    public Long getLikesCount(String shortId) {
        var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
        List<Long> res = DB.sql(query, Long.class);
        return res.get(0);
    }
    @Override
    public Result<Short> getShort(String shortId) {
        return getOne(shortId, Short.class);
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        var query = format("SELECT s.shortId FROM Short s WHERE s.ownerId = '%s'", userId);
        return ok(DB.sql( query, String.class));
    }

    @Override
    public Result<Following> follow(Following f, boolean isFollowing) {
        return isFollowing ? DB.insertOne( f ) : DB.deleteOne( f );
    }

    @Override
    public Result<List<String>> followers(String userId) {
        var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
        return ok(DB.sql(query, String.class));
    }

    @Override
    public Result<Likes> like(Likes l, boolean isLiked) {
        return isLiked ? DB.insertOne( l ) : DB.deleteOne( l );
    }

    @Override
    public Result<List<String>> likes(String shortId) {
        var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);
        return ok(DB.sql(query, String.class));
    }

    @Override
    public Result<List<String>> getFeed(String userId) {
        final var QUERY_FMT = """
				SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'				
				UNION			
				SELECT s.shortId, s.timestamp FROM Short s, Following f 
					WHERE 
						f.followee = s.ownerId AND f.follower = '%s' 
				ORDER BY s.timestamp DESC""";
        return  ok(DB.sql( format(QUERY_FMT, userId, userId), String.class));
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String token) {
        return DB.transaction( (hibernate) -> {

            //delete shorts
            var query1 = format("DELETE Short s WHERE s.ownerId = '%s'", userId);
            hibernate.createMutationQuery(query1).executeUpdate();

            //delete follows
            var query2 = format("DELETE Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
            hibernate.createMutationQuery(query2).executeUpdate();

            //delete likes
            var query3 = format("DELETE Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
            hibernate.createMutationQuery(query3).executeUpdate();

        });
    }
}
