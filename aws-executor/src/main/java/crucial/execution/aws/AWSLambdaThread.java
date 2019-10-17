package crucial.execution.aws;

import com.amazonaws.services.lambda.model.InvokeResult;
import crucial.execution.CloudThread;

import java.util.Base64;

public class AWSLambdaThread extends CloudThread {

    private static AWSLambdaInvoker invoker = new AWSLambdaInvoker(Config.region, Config.functionName);

    public AWSLambdaThread(Runnable target) {
        super(target);
    }

    @Override
    protected void invoke(byte[] threadCall) {
        System.out.println(this.printPrefix() + "Calling AWS Lambda.");
        InvokeResult result = invoker.invoke(threadCall);
        System.out.println(this.printPrefix() + "AWS call completed.");
        if (logs) {
            System.out.println(this.printPrefix() + "Showing Lambda Tail Logs.\n");
            assert result != null;
            System.out.println(new String(Base64.getDecoder().decode(result.getLogResult())));
        }
    }

    public static void closeInvoker(){
        invoker.stop();
    }
}
