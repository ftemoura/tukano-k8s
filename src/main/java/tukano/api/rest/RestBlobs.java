package tukano.api.rest;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.rest.filters.auth.AuthRequired;

@Path(RestBlobs.PATH)
public interface RestBlobs {
	
	String PATH = "/blobs";
	String BLOB_ID = "blobId";
	String TOKEN = "token";
	String BLOBS = "blobs";
	String USER_ID = "userId";

 	@POST
 	@Path("/{" + BLOB_ID +"}")
 	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	void upload(@PathParam(BLOB_ID) String blobId, byte[] bytes, @QueryParam(TOKEN) String token);

 	@GET
 	@Path("/{" + BLOB_ID +"}") 	
 	@Produces(MediaType.APPLICATION_OCTET_STREAM)
 	byte[] download(@PathParam(BLOB_ID) String blobId);

	@AuthRequired
	@DELETE
	@Path("/{" + BLOB_ID + "}")
	void delete(@Context SecurityContext sc, @PathParam(BLOB_ID) String blobId);

	@AuthRequired
	@DELETE
	@Path("/{" + USER_ID + "}/" + BLOBS)
	void deleteAllBlobs(@Context SecurityContext sc, @PathParam(USER_ID) String userId);
}
