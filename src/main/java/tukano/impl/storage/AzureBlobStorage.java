package tukano.impl.storage;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClientBuilder;
import tukano.api.Result;

import java.util.Arrays;
import java.util.function.Consumer;

import utils.ConfigLoader;
import utils.Hash;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.error;
import static tukano.api.Result.ok;


public class AzureBlobStorage implements BlobStorage {
    private BlobContainerClient containerClient;

    public AzureBlobStorage() {
        String blobsContainerName = ConfigLoader.getInstance().getblobShortsName();
        String storageConnectionString = ConfigLoader.getInstance().getBlobConnectionString();
        try {
            this.containerClient = new BlobContainerClientBuilder()
                    .connectionString(storageConnectionString)
                    .containerName(blobsContainerName)
                    .buildClient();
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to initialize Azure Blob Storage: " + e.getMessage());
        }
    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null) {
            return error(BAD_REQUEST);
        }

        BlobClient blobClient = containerClient.getBlobClient(path);

        if (blobClient.exists()) {
            byte[] existingBytes = blobClient.downloadContent().toBytes();

            if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(existingBytes))) {
                return Result.ok();
            } else {
                return Result.error(CONFLICT);
            }
        }

        blobClient.upload(BinaryData.fromBytes(bytes));
        return Result.ok();
    }

    @Override
    public Result<Void> delete(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);
        if (!blob.exists())
            return error(NOT_FOUND);

        blob.delete();
        return Result.ok();
    }

    @Override
    public Result<byte[]> read(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        BlobClient blob = containerClient.getBlobClient(path);
        if (!blob.exists())
            return error(NOT_FOUND);

        BinaryData data = blob.downloadContent();

        byte[] arr = data.toBytes();
        return arr != null ? ok(arr) : error(INTERNAL_ERROR);
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        Result<byte[]> data = read(path);
        if (!data.isOK())
            return error(data.error());

        sink.accept(data.value());
        return Result.ok();
    }
}
