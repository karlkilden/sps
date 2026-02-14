package com.kildeen.sps.publish;

import com.kildeen.sps.Client;
import com.kildeen.sps.IdWithReceipts;
import com.kildeen.sps.IdWithReceiptsResult;
import com.kildeen.sps.Receipt;
import com.kildeen.sps.SpsEvents;
import com.kildeen.sps.json.JsonProvider;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.kildeen.sps.Receipt.ACK;
import static com.kildeen.sps.Receipt.NACK;

public class HttpClient implements Client {
    private static final int QUEUE_CAPACITY = 1000;
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
            .executor(executor)
            .version(java.net.http.HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    @Override
    public CompletableFuture<IdWithReceiptsResult> post(Subscriptions.Subscription subscription, SpsEvents spsEvents) {
        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(JsonProvider.json().write(spsEvents)))
                .uri(URI.create(subscription.url()))
                .header("Content-Type", "application/json")
                .build();

        CompletableFuture<HttpResponse<String>> response =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        final Function<HttpResponse<String>, IdWithReceiptsResult> mapResponse = createReceiptsMapper();
        return response.thenApply(mapResponse);
    }

    @Override
    public EnumSet<DeliveryType> supports() {
        return EnumSet.of(DeliveryType.HTTP);
    }

    private Function<HttpResponse<String>, IdWithReceiptsResult> createReceiptsMapper() {
        return resp -> {
            if (resp.statusCode() == 204) {
                return new IdWithReceiptsResult() {
                    @Override
                    public Receipt allEvents() {
                        return ACK;
                    }

                    @Override
                    public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                        throw new RuntimeException("Not implemented");
                    }

                };
            }
            if (resp.statusCode() == 200) {
                return (IdWithReceiptsResult) () -> JsonProvider.json().readValue(resp.body(), IdWithReceipts.class)
                        .idWithReceipts();

            }
            return new IdWithReceiptsResult() {
                @Override
                public Receipt allEvents() {
                    return NACK;
                }

                @Override
                public List<IdWithReceipts.IdWithReceipt> idWithReceipts() {
                    throw new RuntimeException("Not implemented");
                }

            };
        };
    }

}
