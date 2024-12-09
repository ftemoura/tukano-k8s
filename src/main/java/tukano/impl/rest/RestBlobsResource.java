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
	public void upload(String blobId, byte[] bytes, String token) {
		super.resultOrThrow( impl.upload(blobId, bytes, token));
	}

	@Override
	public byte[] download(String blobId) {
		return super.resultOrThrow( impl.download(blobId));
	}

	@Override
	public void delete(SecurityContext sc, String blobId) {
		super.resultOrThrow( impl.delete(sc, blobId));
	}
	
	@Override
	public void deleteAllBlobs(SecurityContext sc, String userId) {
		super.resultOrThrow( impl.deleteAllBlobs(sc, userId));
	}
}
