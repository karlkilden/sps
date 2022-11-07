package com.kildeen.sps.publish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kildeen.sps.SpsEvents;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HttpSender {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 2, 60, TimeUnit.SECONDS, new SynchronousQueue<>());

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .executor(executor)
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CompletableFuture<HttpResponse<String>> send(PublishableEvent fork) {
        HttpRequest request = null;

        try {
            SpsEvents spsEvents = new SpsEvents(fork.subscription().eventType(), fork.forkedEvents());
            String events = MAPPER.writeValueAsString(spsEvents);

            request = HttpRequest.newBuilder()
                    .POST(HttpRequest.BodyPublishers.ofString(events))
                    .uri(URI.create(fork.subscription().url()))
                    .header("Content-Type", "application/json")
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        CompletableFuture<HttpResponse<String>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        return response;
    }
}
