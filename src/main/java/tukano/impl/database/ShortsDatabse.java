package tukano.impl.database;

import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.User;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;

import java.util.List;

public interface ShortsDatabse {
    Result<Short> createShort(Short shrt);
    Result<Void> deleteShort(Short shrt, User deletedBy);
    Long getLikesCount(String shortId);
    Result<Short> getShort(String shortId);
    Result<List<String>> getShorts(String userId );
    Result<Following> follow(Following f, boolean isFollowing);
    Result<List<String>> followers(String userId);
    Result<Likes> like(Likes l, boolean isLiked);
    Result<List<String>> likes(String shortId);
    Result<List<String>> getFeed(String userId);
    Result<Void> deleteAllShorts(String userId);
    Result<Void> updateShortViews(String shortId, Long views);

}
