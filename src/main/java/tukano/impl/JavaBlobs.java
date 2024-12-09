package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import jakarta.ws.rs.core.SecurityContext;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.clients.RestShortsClient;
import tukano.impl.cache.RedisCacheBlobs;
import tukano.impl.storage.BlobStorage;
import tukano.impl.storage.FilesystemStorage;
import utils.Hash;
import utils.Hex;

public class JavaBlobs implements Blobs {
	private static Blobs instance;
	private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());
	private final RestShortsClient shorts;
	private BlobStorage storage;
	private RedisCacheBlobs cache;
	private static final Long MAX_TIME_WITHOUT_UPDATE = 120000L;//2min

	synchronized public static Blobs getInstance() {
		if( instance == null )
			instance = new JavaBlobs();
		return instance;
	}
	
	private JavaBlobs() {
		storage = new FilesystemStorage();
		cache = new RedisCacheBlobs();
		shorts = new RestShortsClient();
	}
	
	@Override
	public Result<Void> upload(String blobId, byte[] bytes, String token) {
		Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)), token));
		
		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		return storage.write( toPath( blobId ), bytes);
	}

	@Override
	public Result<byte[]> download(String blobId) {
		Log.info(() -> format("download : blobId = %s\n", blobId));
		Executors.defaultThreadFactory().newThread(() -> {
			Result<Long> viewsRes = cache.getBlobViews(blobId);
			Long views = 0L;
			if (!viewsRes.isOK()) {
				Result<Short> shrtRes = shorts.getShort(blobId);
				if (!shrtRes.isOK())
					return;
				views = shrtRes.value().getViews() + 1;
				cache.setBlobViews(blobId, views);

			} else {
				Result<Long> vr = cache.increaseBlobViews(blobId);
				if (vr.isOK())
					views = vr.value();
			}

			Result<Long> lastUpdateRes = cache.getLastUpdate(blobId);
			if (!lastUpdateRes.isOK()) {
				cache.updateLastUpdate(blobId, System.currentTimeMillis());
			} else {
				if (System.currentTimeMillis() - lastUpdateRes.value() > MAX_TIME_WITHOUT_UPDATE)
					shorts.updateShortViews(blobId, views);
				cache.updateLastUpdate(blobId, System.currentTimeMillis());
			}
		}).start();

		return storage.read( toPath( blobId ) );
	}

	@Override
	public Result<Void> downloadToSink(String blobId, Consumer<byte[]> sink) {
		Log.info(() -> format("downloadToSink : blobId = %s\n", blobId));
		return storage.read( toPath(blobId), sink);
	}

	@Override
	public Result<Void> delete(SecurityContext sc, String blobId) {
		Log.info(() -> format("delete : blobId = %s\n", blobId));
		if( !sc.isUserInRole(Token.Role.ADMIN.toString())) {
			Log.info(() -> format("FORBIDDEN : Attempted to delete all blobs as a non-admin user, id: %s role: %s",
					Token.getSubject(sc.getUserPrincipal().getName()),
					Token.getClaim(sc.getUserPrincipal().getName(), "role")));
			return error(FORBIDDEN);
		}
		return storage.delete( toPath(blobId));
	}
	
	@Override
	public Result<Void> deleteAllBlobs(SecurityContext sc, String userId) {
		Log.info(() -> format("deleteAllBlobs : userId = %s\n", userId));
		if( !sc.isUserInRole(Token.Role.ADMIN.toString())) {
			Log.info(() -> format("FORBIDDEN : Attempted to delete all blobs as a non-admin user, id: %s role: %s",
					Token.getSubject(sc.getUserPrincipal().getName()),
					Token.getClaim(sc.getUserPrincipal().getName(), "role")));
			return error(FORBIDDEN);
		}
		return storage.deleteFolder( toPath(userId));
	}
	
	private boolean validBlobId(String blobId, String token) {
		return Token.isValid(token, Token.Service.BLOBS, blobId);
	}

	private String toPath(String blobId) {
		return blobId.replace("+", "/");
	}
}
