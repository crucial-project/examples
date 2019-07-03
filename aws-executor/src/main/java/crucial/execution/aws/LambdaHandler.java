package crucial.execution.aws;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lambda handler for CloudThreads.
 *
 * @author Daniel
 */
public class LambdaHandler implements RequestHandler<HashMap<String, String>, Object> {

    @Override
    public final Object handleRequest(HashMap<String, String> input, Context context) {
        String threadName = input.get(CloudThread.threadNameKey);
        System.out.println("[H x " + threadName + "] Lambda thread starts.");
        String targetClassName = input.get(CloudThread.targetClassNameKey);
        Runnable run;
        try {
            Class klss = Class.forName(targetClassName);
            System.out.println("[H x " + threadName + "] Class " + targetClassName + " acquired.");
            if (Runnable.class.isAssignableFrom(klss)) {
                run = (Runnable) klss.newInstance();
                System.out.println("[H x " + threadName + "] Class instanced.");

                // Get field annotated with @Keep
                List<Field> keepFields = Arrays.stream(klss.getDeclaredFields())
                        .filter((f) -> f.getAnnotationsByType(Keep.class).length > 0)
                        .collect(Collectors.toList());
                for (Field field : keepFields) {
                    try {
                        field.setAccessible(true);
                        field.set(run, Serializer.deserialize(input.get(field.getName())));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
                run.run();
                System.out.println("[H x " + threadName + "] Class run successfully.");
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
