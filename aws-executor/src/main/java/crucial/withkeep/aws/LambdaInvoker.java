package crucial.withkeep.aws;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.LogType;

/**
 * Invoke calls to AWS Lambda.
 *
 * @author Daniel
 */
public class LambdaInvoker {
    private static AWSLambda lambdaClient;
    private String lambdaFunctionName;

    public LambdaInvoker(Regions region, String functionName) {
        lambdaClient = AWSLambdaClientBuilder.standard()
                .withRegion(region)
                .withClientConfiguration(new ClientConfiguration()
                        .withMaxConnections(1000).withSocketTimeout(600_000))
                .build();
        lambdaFunctionName = functionName;
    }

    /**
     * Synchronous Lambda invocation. With tail logs.
     *
     * @param payload Input for the Lambda
     * @return InvokeResult of the call.
     */
    public InvokeResult invoke(String payload) {
        return invoke(payload, true);
    }

    public InvokeResult invoke(String payload, boolean tail) {
        InvokeRequest req = new InvokeRequest();
        req.setFunctionName(lambdaFunctionName);
        req.setInvocationType(InvocationType.RequestResponse);
        req.setPayload(payload);
        if (tail) req.setLogType(LogType.Tail);
        return lambdaClient.invoke(req);
    }

    public void stop() {
        lambdaClient.shutdown();
    }
}
