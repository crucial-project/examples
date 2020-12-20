package org.crucial.examples;

import org.crucial.dso.MergeableMap;
import org.crucial.dso.Sum;
import org.crucial.dso.client.Client;
import org.crucial.executor.ServerlessExecutorService;
import org.crucial.executor.aws.AWSLambdaExecutorService;
import org.testng.annotations.Test;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class CountURLs {

    private final static String URLsFile = "urls.txt";

    @Test
    public void test() {

        try {
            List<Callable<Void>> tasks = Collections.synchronizedList(new ArrayList<>());
            Client client = Client.getClient();
            MergeableMap<String, Long> urls = client.getAtomicMap("urls");
            ServerlessExecutorService es = new AWSLambdaExecutorService();
            Files.lines(Paths.get(new File(this.getClass().getClassLoader().getResource(URLsFile).getFile()).toURI()))
                    .forEach(x -> tasks.add(
                            (Callable<Void> & Serializable)
                                    () -> {
                                        urls.mergeAll(parseURL(x), new Sum());
                                        return null;
                                    })
                    );
            es.invokeAll(tasks);
            System.out.println(urls.size());

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static Map<String, Long> parseURL(String url) {
        Map<String, Long> tmp = new HashMap<>();
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setConnectTimeout(1000);
            connection.connect();

            // count words
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            reader.lines()
                    .flatMap(line -> Stream.of(line.toLowerCase().split("\\W+"))
                            .filter(w -> !w.isEmpty()))
                    .forEach(word -> tmp.merge(word, 1L, Long::sum));
        }catch (Exception e){
            System.err.println(url);
        }
        return tmp;
    }



}
