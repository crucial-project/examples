package crucial.execution.aws;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs a Runnable on Amazon Lambda.
 *
 * @author Daniel
 */
public class CloudThread extends Thread {
    // === === === === === === === === === ===
    //             CONFIGURE THIS
    private static Regions region = Regions.US_EAST_1;
    private static String functionName = "CloudThread-example";
    // === === === === === === === === === ===
    static final int maxTries = 1;
    static final String targetClassNameKey = "targetClassName";
    static final String threadNameKey = "threadName";
    private static Gson gson = new Gson();
    private static LambdaInvoker invoker = new LambdaInvoker(region, functionName);
    private Runnable lambdaTarget;
    private boolean local = false;
    private boolean logs = true;

    public CloudThread(Runnable target) {
        this.setName("Lambda-" + this.getName());
        lambdaTarget = target;
    }

    public static void stopInvoker() {
        invoker.stop();
    }

    /**
     * AWS Lambda call abstraction logic.
     */
    @Override
    public void run() {
        System.out.println("[T x " + this.getName() + "] Thread starts to run.");
        Class klass = lambdaTarget.getClass();
        if (isInnerClass(klass) && !isStaticClass(klass)) {
            throw new RuntimeException("[T x " + this.getName() + "] "
                    + "Illegal class definition for Lambda. Cannot be inner unless static.");
        } // else : legal class definition
        if (!hasEmptyConstructor(klass)) {
            throw new RuntimeException("[T x " + this.getName() + "] "
                    + "Illegal class definition for Lambda. It must have an empty constructor.");
        }
        // Get field annotated with @Keep
        List<Field> keepFields = Arrays.stream(lambdaTarget.getClass().getDeclaredFields())
                .filter((f) -> f.getAnnotationsByType(Keep.class).length > 0)
                .collect(Collectors.toList());

        HashMap<String, String> input = new HashMap<>();
        input.put(threadNameKey, this.getName());
        input.put(targetClassNameKey, klass.getName());
        for (Field field : keepFields) {
            try {
                field.setAccessible(true);
                input.put(field.getName(), Serializer.serialize(field.get(lambdaTarget)));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        InvokeResult result;
        if (local) {
            System.out.println("[T x " + this.getName() + "] Running Lambda Thread in local emulation.");
            LambdaHandler handler = new LambdaHandler();
            handler.handleRequest(input, null);
            result = null;
        } else {
            int tries = 0;
            do {
                System.out.println("[T x " + this.getName() + "] Running Lambda Thread.");
                result = invoker.invoke(gson.toJson(input));
                if (result.getFunctionError() == null) break;
                tries++;
                System.out.println("[T x " + this.getName() + "] Lambda failed. ERROR:");
                System.out.println("[T x " + this.getName() + "] "
                        + new String(result.getPayload().array(), StandardCharsets.UTF_8));
                System.out.println("[T x " + this.getName() + "] Retries: " + (tries - 1)
                        + (tries >= maxTries ? " Dropping..." : " Retrying..."));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (tries < maxTries);
        }
        System.out.println("[T x " + this.getName() + "] Lambda Thread completed.");
        if (!local && logs) {
            System.out.println("[T x " + this.getName() + "] Showing Lambda Tail Logs.\n");
            assert result != null;
            System.out.println(new String(Base64.getDecoder().decode(result.getLogResult())));
            System.out.println("[T x " + this.getName() + "] Lambda return payload:");
            System.out.println("[T x " + this.getName() + "] "
                    + new String(result.getPayload().array(), StandardCharsets.UTF_8));
        }

        System.out.println("[T x " + this.getName() + "] Exiting Lambda Thread.");
    }

    /**
     * Check if the given class is an inner class. Defined inside another
     * class.
     *
     * @param k The class to check.
     * @return True if the class is inner. False otherwise.
     */
    private boolean isInnerClass(Class k) {
        return k.getEnclosingClass() != null;
    }

    /**
     * Check if the given class is static.
     *
     * @param k The class to check.
     * @return True if the class is static. False otherwise.
     */
    private boolean isStaticClass(Class k) {
        return Modifier.isStatic(k.getModifiers());
    }

    private boolean hasEmptyConstructor(Class<?> clazz) {
        return Stream.of(clazz.getConstructors())
                .anyMatch((c) -> c.getParameterCount() == 0);
    }

    /**
     * Sets te running mode. *true* means that no actual Lambda is run,
     * the execution is simulates locally with threads.
     * @param local
     */
    public void setLocal(boolean local) {
        this.local = local;
    }
}
