package tukano.impl.functions;

import java.util.*;
import java.util.logging.Logger;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.impl.JavaBlobs;
import tukano.impl.JavaShorts;
import tukano.impl.Token;
import tukano.impl.database.ShortsDatabse;
import utils.ConfigLoader;

import static tukano.api.Result.statusFromErrorCode;

/**
 * Azure Functions with HTTP Trigger.
 */
public class DownloadBlobs {
    private static final String BLOB_ID = "blobId";
    private static final String TOKEN = "token";
    private static final String HTTP_TRIGGER_NAME = "req";
    private static final String HTTP_FUNCTION_NAME = "DownloadBlob";
    private static final String HTTP_TRIGGER_ROUTE = "blobs/{" + BLOB_ID + "}";


    // Instance of JavaBlobs to interact with blob storage
    private final Blobs javaBlobs = JavaBlobs.getInstance();

    static {
        Token.setSecret(ConfigLoader.getInstance().getTokenSecret());
    }

    @FunctionName(HTTP_FUNCTION_NAME)
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = HTTP_TRIGGER_NAME,
                    methods = {HttpMethod.GET},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = HTTP_TRIGGER_ROUTE
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName(BLOB_ID) String blobId,
            final ExecutionContext context) {

        context.getLogger().info("Download request received for blob ID: " + blobId);
        String token = request.getQueryParameters().get(TOKEN);
        context.getLogger().info("Token: " + token);
        try {
            Result<byte[]> result = javaBlobs.download(blobId, token);
            if (!result.isOK()) {
                context.getLogger().warning("Download failed: Forbidden or Blob not found.");
                return request.createResponseBuilder(statusFromErrorCode(result.error()))
                        .build();
            }
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + blobId + "\"")
                    .body(result.value())
                    .build();
        } catch (Exception e) {
            context.getLogger().severe("Error during download: " + e.getMessage());
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error occurred.")
                    .build();
        }
    }
}