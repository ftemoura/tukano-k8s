package tukano.impl.rest;

import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Blobs;
import tukano.api.rest.RestBlobs;
import tukano.impl.JavaBlobs;

@Singleton
public class RestBlobsResource extends RestResource implements RestBlobs {

	final Blobs impl;
	
	public RestBlobsResource() {
		this.impl = JavaBlobs.getInstance();
	}
	
	@Override
	public void upload(SecurityContext sc, String blobId, byte[] bytes, String token) {
		super.resultOrThrow( impl.upload(sc, blobId, bytes, token));
	}

	@Override
	public byte[] download(SecurityContext sc, String blobId, String token) {
		return super.resultOrThrow( impl.download(sc, blobId, token ));
	}

	@Override
	public void delete(SecurityContext sc, String blobId, String token) {
		super.resultOrThrow( impl.delete(sc, blobId, token ));
	}
	
	@Override
	public void deleteAllBlobs(SecurityContext sc, String userId, String password) {
		super.resultOrThrow( impl.deleteAllBlobs(sc, userId, password ));
	}
}
