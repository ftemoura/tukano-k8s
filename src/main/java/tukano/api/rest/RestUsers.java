package tukano.api.rest;

import java.util.List;

import com.azure.core.http.HttpHeader;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tukano.api.User;
import tukano.api.rest.filters.auth.AuthRequired;


@Path(RestUsers.PATH)
public interface RestUsers {




	String PATH = "/users";

	String PWD = "pwd";
	String QUERY = "query";
	String USER_ID = "userId";
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	String createUser(User user);

	@POST
	@Path("/login")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	Response login(@FormParam(USER_ID) String userId, @FormParam(PWD) String pwd);


	@GET
	@AuthRequired
	@Path("/{" + USER_ID+ "}")
	@Produces(MediaType.APPLICATION_JSON)
	User getUser(@Context SecurityContext sc,  @PathParam(USER_ID) String userId);
	
	
	@PUT
	@AuthRequired
	@Path("/{" + USER_ID+ "}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	User updateUser(@Context SecurityContext sc, @PathParam( USER_ID ) String userId, User user);
	
	
	@DELETE
	@AuthRequired
	@Path("/{" + USER_ID+ "}")
	@Produces(MediaType.APPLICATION_JSON)
	User deleteUser(@Context SecurityContext sc, @PathParam(USER_ID) String userId);
	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	List<User> searchUsers(@QueryParam(QUERY) String pattern);	
}
