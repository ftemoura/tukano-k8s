package tukano.impl.functions;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import tukano.api.Blobs;
import tukano.api.Result;
import tukano.impl.JavaBlobs;
import tukano.impl.Token;

import java.util.Arrays;
import java.util.Optional;

import static tukano.api.Result.statusFromErrorCode;

public class UploadBlobs {
    private static final String BLOB_ID = "blobId";
    private static final String TOKEN = "token";
    private static final String HTTP_TRIGGER_NAME = "req";
    private static final String HTTP_FUNCTION_NAME = "UploadBlob";
    private static final String HTTP_TRIGGER_ROUTE = "blobs/{" + BLOB_ID + "}";


    // Instance of JavaBlobs to interact with blob storage
    private final Blobs javaBlobs = JavaBlobs.getInstance();

    static {
        Token.setSecret("secret");//TODO
    }
    @FunctionName(HTTP_FUNCTION_NAME)
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = HTTP_TRIGGER_NAME,
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = HTTP_TRIGGER_ROUTE
            ) HttpRequestMessage<Optional<byte[]>> request,
            @BindingName(BLOB_ID) String blobId,
            final ExecutionContext context) {

        context.getLogger().info("Download request received for blob ID: " + blobId);
        String token = request.getQueryParameters().get(TOKEN);
        context.getLogger().info("Token: " + token);
        if (request.getBody().isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Request body is missing.")
                    .build();
        }

        byte[] bytes = request.getBody().get();
        context.getLogger().info("Bytes: " + bytes.length +" "+ Arrays.toString(bytes));

        try {
            Result<Void> result = javaBlobs.upload(blobId, bytes, token);
            if (!result.isOK()) {
                context.getLogger().warning("Upload failed: Forbidden or Blob not found.");
                return request.createResponseBuilder(statusFromErrorCode(result.error()))
                        .build();
            }
            return request.createResponseBuilder(HttpStatus.OK)
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
