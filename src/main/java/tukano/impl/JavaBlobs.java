package tukano.impl;

import static java.lang.String.format;
import static tukano.api.Result.error;
import static tukano.api.Result.ErrorCode.FORBIDDEN;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;

import tukano.api.Blobs;
import tukano.api.Result;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.impl.cache.RedisCacheBlobs;
import tukano.impl.rest.MainApplication;
import tukano.impl.rest.TukanoRestServer;
import tukano.impl.storage.AzureBlobStorage;
import tukano.impl.storage.BlobStorage;
import tukano.impl.storage.FilesystemStorage;
import utils.Hash;
import utils.Hex;

public class JavaBlobs implements Blobs {
	private static Blobs instance;
	private static Logger Log = Logger.getLogger(JavaBlobs.class.getName());
	private final Shorts javaShorts;
	public String baseURI;
	private BlobStorage storage;
	private RedisCacheBlobs cache;
	private static final Long MAX_TIME_WITHOUT_UPDATE =120000L;//2min

	synchronized public static Blobs getInstance() {
		if( instance == null )
			instance = new JavaBlobs();
		return instance;
	}
	
	private JavaBlobs() {
		storage = new AzureBlobStorage();
		cache = new RedisCacheBlobs();
		javaShorts = JavaShorts.getInstance();
		baseURI = String.format("%s/%s/", MainApplication.serverURI, Blobs.NAME);
	}
	
	@Override
	public Result<Void> upload(String blobId, byte[] bytes, String token) {
		Log.info(() -> format("upload : blobId = %s, sha256 = %s, token = %s\n", blobId, Hex.of(Hash.sha256(bytes)), token));

		if (!validBlobId(blobId, token))
			return error(FORBIDDEN);

		return storage.write( toPath( blobId ), bytes);
	}

	@Override
	public Result<byte[]> download(String blobId, String token) {
		Log.info(() -> format("download : blobId = %s, token=%s\n", blobId, token));

		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);
		Executors.defaultThreadFactory().newThread(() -> {
			Result<Long> viewsRes = cache.getBlobViews(blobId);
			Long views = 0L;
			if (!viewsRes.isOK()) {
				Result<Short> shrtRes = javaShorts.getShort(blobId);
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
					javaShorts.updateShortViews(blobId, views);
				cache.updateLastUpdate(blobId, System.currentTimeMillis());
			}
		}).start();

		return storage.read( toPath( blobId ) );
	}

	@Override
	public Result<Void> downloadToSink(String blobId, Consumer<byte[]> sink, String token) {
		Log.info(() -> format("downloadToSink : blobId = %s, token = %s\n", blobId, token));

		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);

		return storage.read( toPath(blobId), sink);
	}

	@Override
	public Result<Void> delete(String blobId, String token) {
		Log.info(() -> format("delete : blobId = %s, token=%s\n", blobId, token));
	
		if( ! validBlobId( blobId, token ) )
			return error(FORBIDDEN);

		return storage.delete( toPath(blobId));
	}
	
	@Override
	public Result<Void> deleteAllBlobs(String userId, String token) {
		Log.info(() -> format("deleteAllBlobs : userId = %s, token=%s\n", userId, token));

		if( ! Token.isValid( token, Token.Service.INTERNAL, userId ) )
			return error(FORBIDDEN);
		
		return storage.deleteFolder( toPath(userId));
	}
	
	private boolean validBlobId(String blobId, String token) {		
		System.out.println( blobId);
		return Token.isValid(token, Token.Service.BLOBS, blobId);
	}

	private String toPath(String blobId) {
		return blobId.replace("+", "/");
	}
}
