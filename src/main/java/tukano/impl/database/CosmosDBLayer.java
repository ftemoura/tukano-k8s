package tukano.impl.database;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import tukano.api.Result;
import utils.ConfigLoader;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.errorCodeFromStatus;

public abstract class CosmosDBLayer {
    private static Logger Log = Logger.getLogger(CosmosDBLayer.class.getName());
    private static final String CONNECTION_URL = ConfigLoader.getInstance().getCosmosConnectionUrl();
    private static final String DB_KEY = ConfigLoader.getInstance().getCosmosDBKey();
    private static final String DB_NAME = ConfigLoader.getInstance().getCosmosDBName();
    private final String containerName;
    private final CosmosClient client;
    private CosmosDatabase db;
    private CosmosContainer container;

    public CosmosDBLayer(String containerName) {
        this.client = new CosmosClientBuilder().endpoint(CONNECTION_URL).key(DB_KEY)
                //.directMode()
                .gatewayMode()
                // replace by .directMode() for better performance
                .consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true).contentResponseOnWriteEnabled(true).buildClient();
        this.containerName = containerName;
    }

    private synchronized void init() {
        if (db != null) return;
        db = client.getDatabase(DB_NAME);
        container = db.getContainer(this.containerName);
    }

    public void close() {
        client.close();
    }

    public <T> Result<T> getOne(String id, Class<T> clazz) {
        return tryCatch(() -> container.readItem(id, new PartitionKey(id), clazz).getItem());
    }

    public <T> Result<T> deleteOne(T obj, String etag) {
        return tryCatch(() -> retry(() -> {
            Log.info(() -> format("deleteOne : %s\n", obj));
            container.deleteItem(obj, new CosmosItemRequestOptions().setIfMatchETag(etag));
            return obj;
        }, 3, 1000));
    }

    public <T> Result<T> updateOne(T obj) {
        return tryCatch(() -> container.upsertItem(obj).getItem());
    }

    public <T> Result<T> insertOne(T obj) {
        return tryCatch(() -> container.createItem(obj).getItem());
    }

    public <T> Result<List<T>> query(String queryStr, Class<T> clazz) {
        return tryCatch(() -> {
            var res = container.queryItems(queryStr, new CosmosQueryRequestOptions(), clazz);
            return res.stream().toList();
        });
    }

    <T> Result<T> tryCatch(Supplier<T> supplierFunc) {
        try {
            init();
            return Result.ok(supplierFunc.get());
        } catch (CosmosException ce) {
            ce.printStackTrace();
            return Result.error(errorCodeFromStatus(ce.getStatusCode()));
        } catch (Exception  x ) {
            x.printStackTrace();
            return Result.error(INTERNAL_ERROR);
        }
    }

    public static <T> T retry(Supplier<T> task, int maxRetries, long delay)  {
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                return task.get();
            } catch (CosmosException e) {
                if (errorCodeFromStatus(e.getStatusCode()) == PRECONDITION_FAILED) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    System.out.println("Attempt " + attempt + " failed: " + e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }else
                    throw e;
            }
        }
        throw new RuntimeException("Retry failed");
    }

}