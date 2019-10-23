package crucial.execution;

import java.io.IOException;
import java.util.concurrent.Callable;

public class CloudThreadHandler {

    protected byte[] handle(byte[] input) {
        Object result = null;
        try {
            ThreadCall call = ByteMarshaller.fromBytes(input);
            System.out.println(call.getThreadName() + " loaded.");
            Callable c = call.getTarget();
            result = c.call();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            result = e;
        }
        try {
            return ByteMarshaller.toBytes(result);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
