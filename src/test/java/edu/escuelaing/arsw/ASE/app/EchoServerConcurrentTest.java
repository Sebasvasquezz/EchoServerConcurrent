package edu.escuelaing.arsw.ASE.app;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class EchoServerConcurrentTest {

    private static ExecutorService executorService;
    private static final int NUM_REQUESTS = 10;
    private static final String SERVER_URL = "http://localhost:35000";

    @BeforeAll
    public static void setUp() throws IOException {
        // Start the web server in a separate thread
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try {
                EchoServerConcurrent.main(new String[]{});
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Wait 1 second to make sure the server is running
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    public void testConcurrentRequests() throws InterruptedException, ExecutionException {
        ExecutorService testExecutor = Executors.newFixedThreadPool(NUM_REQUESTS);
        HttpClient client = HttpClient.newHttpClient();

        Callable<Boolean> requestTask = () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(SERVER_URL + "/index.html"))
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                System.out.println("Status Code: " + response.statusCode());

                // Check that the response contains part of the content of the index.html file
                String expectedContentSnippet = "<title>Form Example</title>";
                return response.statusCode() == 200 && response.body().contains(expectedContentSnippet);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        };

        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_REQUESTS; i++) {
            futures.add(testExecutor.submit(requestTask));
        }

        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "All requests should be successful and return the expected content");
        }

        testExecutor.shutdown();
    }
}
