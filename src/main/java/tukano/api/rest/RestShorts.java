package tukano.api.rest;

import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Short;
import tukano.api.rest.filters.auth.AuthRequired;

@Path(RestShorts.PATH)
public interface RestShorts {
	String PATH = "/shorts";
	
	String USER_ID = "userId";
	String USER_ID1 = "userId1";
	String USER_ID2 = "userId2";
	String SHORT_ID = "shortId";
	
	String PWD = "pwd";
	String FEED = "/feed";
	String TOKEN = "token";
	String LIKES = "/likes";
	String SHORTS = "/shorts";
	String FOLLOWERS = "/followers";

	String VIEWS = "/views";
	
	@POST
	@AuthRequired
	@Path("/{" + USER_ID + "}")
	@Produces(MediaType.APPLICATION_JSON)
	Short createShort(@Context SecurityContext sc, @PathParam(USER_ID) String userId);

	@DELETE
	@AuthRequired
	@Path("/{" + SHORT_ID + "}")
	void deleteShort(@Context SecurityContext sc, @PathParam(SHORT_ID) String shortId);

	@GET
	@Path("/{" + SHORT_ID + "}" )
	@Produces(MediaType.APPLICATION_JSON)
	Short getShort(@PathParam(SHORT_ID) String shortId);

	@GET
	@Path("/{" + USER_ID + "}" + SHORTS )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> getShorts(@PathParam(USER_ID) String userId);

	@POST
	@AuthRequired
	@Path("/{" + USER_ID1 + "}/{" + USER_ID2 + "}" + FOLLOWERS )
	@Consumes(MediaType.APPLICATION_JSON)
	void follow(@Context SecurityContext sc, @PathParam(USER_ID1) String userId1, @PathParam(USER_ID2) String userId2, boolean isFollowing);

	@GET
	@AuthRequired
	@Path("/{" + USER_ID + "}" + FOLLOWERS )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> followers(@Context SecurityContext sc, @PathParam(USER_ID) String userId);

	@POST
	@AuthRequired
	@Path("/{" + SHORT_ID + "}/{" + USER_ID + "}" + LIKES )
	@Consumes(MediaType.APPLICATION_JSON)
	void like(@Context SecurityContext sc, @PathParam(SHORT_ID) String shortId, @PathParam(USER_ID) String userId, boolean isLiked);

	@GET
	@AuthRequired
	@Path("/{" + SHORT_ID + "}" + LIKES )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> likes(@Context SecurityContext sc, @PathParam(SHORT_ID) String shortId);

	@GET
	@AuthRequired
	@Path("/{" + USER_ID + "}" + FEED )
	@Produces(MediaType.APPLICATION_JSON)
	List<String> getFeed(@Context SecurityContext sc, @PathParam(USER_ID) String userId);
	
	@DELETE
	@AuthRequired
	@Path("/{" + USER_ID + "}" + SHORTS)
	void deleteAllShorts(@PathParam(USER_ID) String userId, @QueryParam(TOKEN) String token);

	@PUT
	//TODO METER UM TOKEN
	@Path("/{" + SHORT_ID + "}" + VIEWS)
	@Consumes(MediaType.APPLICATION_JSON)
	void updateShortViews(@PathParam(SHORT_ID) String shortId, int views);
}
