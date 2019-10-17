package crucial.execution.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;
import com.google.gson.Gson;

import java.nio.ByteBuffer;

/**
 * Invoke calls at AWS Lambda.
 *
 * @author Daniel
 */
public class AWSLambdaInvoker {
    private static AWSLambda lambdaClient;
    private String lambdaFunctionName;
    private Gson gson = new Gson();

    AWSLambdaInvoker(Regions region, String functionName) {
        lambdaClient = AWSLambdaClientBuilder.standard()
                .withRegion(region)
                .withClientConfiguration(new ClientConfiguration()
                        .withMaxConnections(400)
                        .withSocketTimeout(600_000)
                ).build();
        lambdaFunctionName = functionName;
    }

    /**
     * Synchronous Lambda invocation. With tail logs.
     *
     * @param payload Input for the Lambda
     * @return InvokeResult of the call.
     */
    InvokeResult invoke(byte[] payload) {
        return invoke(payload, true);
    }

    /**
     * Synchronous Lambda invocation.
     *
     * @param payload  Input for the Lambda
     * @param tailLogs Request tail logs?
     * @return InvokeResult of the call.
     */
    InvokeResult invoke(byte[] payload, boolean tailLogs) {
        InvokeRequest req = new InvokeRequest();
        req.setFunctionName(lambdaFunctionName);
        req.setInvocationType(InvocationType.RequestResponse);
        req.setPayload(ByteBuffer.wrap(gson.toJson(payload).getBytes()));
        if (tailLogs) req.setLogType(LogType.Tail);
        return lambdaClient.invoke(req);
    }

    void stop() {
        lambdaClient.shutdown();
    }

}
