package crucial.execution.aws;


import com.amazonaws.services.lambda.model.InvokeResult;
import crucial.execution.ServerlessExecutorService;

import java.util.Base64;

public class AWSLambdaExecutorService extends ServerlessExecutorService {
    private AWSLambdaInvoker invoker = new AWSLambdaInvoker(Config.region, Config.functionName);

    @Override
    protected byte[] invokeExternal(byte[] threadCall) {
        System.out.println(this.printPrefix() + "Calling AWS Lambda.");
        InvokeResult result = invoker.invoke(threadCall);
        System.out.println(this.printPrefix() + "AWS call completed.");
        if (logs) {
            System.out.println(this.printPrefix() + "Showing Lambda Tail Logs.\n");
            assert result != null;
            System.out.println(new String(Base64.getDecoder().decode(result.getLogResult())));
        }
        return Base64.getMimeDecoder().decode(result.getPayload().array());
    }

    public void closeInvoker() {
        invoker.stop();
    }
}
