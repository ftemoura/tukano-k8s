package tukano.impl.database;

import com.azure.cosmos.*;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.databind.node.ObjectNode;
import tukano.api.Result;
import utils.ConfigLoader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.errorCodeFromStatus;
import static utils.JSON.mapper;

public abstract class CosmosDBLayer {
    private static Logger Log = Logger.getLogger(CosmosDBLayer.class.getName());
    private static final String CONNECTION_URL = ConfigLoader.getInstance().getCosmosConnectionUrl();
    private static final String DB_KEY = ConfigLoader.getInstance().getCosmosDBKey();
    private static final String DB_NAME = ConfigLoader.getInstance().getCosmosDBName();
    private final CosmosClient client;
    private CosmosDatabase db;
    private final Map<String, CosmosContainer> containers;

    public CosmosDBLayer() {
        this.client = new CosmosClientBuilder().endpoint(CONNECTION_URL).key(DB_KEY)
                //.directMode()
                .gatewayMode()
                // replace by .directMode() for better performance
                .consistencyLevel(ConsistencyLevel.SESSION).connectionSharingAcrossClientsEnabled(true).contentResponseOnWriteEnabled(true).buildClient();
        this.containers = new HashMap<>();
    }

    private synchronized void init() {
        if (db != null) return;
        db = client.getDatabase(DB_NAME);
       // container = db.getContainer(this.containerName);
    }

    public CosmosContainer getContainer(String containerName) {
        init();
        return containers.computeIfAbsent(containerName, name -> db.getContainer(name));
    }

    public void close() {
        client.close();
    }

    public <T> Result<T> getOne(String id, String containerName, Class<T> clazz) {
        return tryCatch(() -> getContainer(containerName).readItem(id, new PartitionKey(id), clazz).getItem());
    }

    public <T> Result<T> deleteOne(T obj, String containerName, String etag) {
        return tryCatch(() -> retry(() -> {
            getContainer(containerName).deleteItem(obj, new CosmosItemRequestOptions().setIfMatchETag(etag));
            return obj;
        }, 3, 1000));
    }

    public <T> Result<T> deleteOne(T obj, String containerName) {
        return tryCatch(() -> {
            getContainer(containerName).deleteItem(obj, new CosmosItemRequestOptions());
            return obj;
        });
    }

    public <T> Result<T> updateOne(T obj, String containerName) {
        return tryCatch(() -> getContainer(containerName).upsertItem(obj).getItem());
    }

    public <T> Result<T> insertOne(T obj, String containerName) {
        return tryCatch(() ->{
            CosmosItemResponse<T> response = getContainer(containerName).createItem(obj);

            // Log the basic response details
            Log.info("Insert Response: " + response);
            Log.info("Response Status Code: " + response.getStatusCode());
            Log.info("Request Charge: " + response.getRequestCharge());

            // Check and log `_ts` value if present
            ObjectNode responseNode = mapper.valueToTree(response.getItem());
            if (responseNode.has("_ts")) {
                Log.info("the User created: " + response.getItem());
                Log.info("_ts value in the response: " + responseNode.get("_ts").asText());
            } else {
                Log.warning("_ts field is not present in the response.");
            }

            return response.getItem();
        });
    }

    public <T> Result<List<T>> query(String queryStr, String containerName, Class<T> clazz) {
        return tryCatch(() -> {
            var res = getContainer(containerName).queryItems(queryStr, new CosmosQueryRequestOptions(), clazz);
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