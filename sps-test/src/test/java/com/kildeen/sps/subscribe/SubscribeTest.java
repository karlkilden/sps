package com.kildeen.sps.subscribe;

import com.kildeen.embeddeddb.EmbeddedDatabase;
import com.kildeen.sps.BasicSpsEvents;
import com.kildeen.sps.JvmLocalPostImpl;
import com.kildeen.sps.SpsEvent;
import com.kildeen.sps.SpsEventType;
import com.kildeen.sps.SpsSubscriberType;
import com.kildeen.sps.TestInit;
import com.kildeen.sps.inlet.Inlet;
import com.kildeen.sps.inlet.InletDI;
import com.kildeen.sps.inlet.Receiver;
import com.kildeen.sps.persistence.DataBaseProvider;
import com.kildeen.sps.publish.PublishDI;
import com.kildeen.sps.publish.RetryPolicies;
import com.kildeen.sps.publish.SameJVMClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.awaitility.Awaitility.await;

public class SubscribeTest {

    static Inlet inlet;
    static PublishDI publish;
    private static SpsEvent receivedEvent;

    static {
        TestInit.init();
    }

    @BeforeAll
    static void setUp() {
        DataBaseProvider.configure(EmbeddedDatabase.get());
        Subscribe subscribe = SubscribeDI.INSTANCE.inject();
        SubscriptionReceiver receiver = new SubscriptionReceiver(subscribe);
        Receiver WantEvent = new Receiver() {

            @Override
            public void receive(SpsEvent spsEvent) {
                receivedEvent = spsEvent;
            }

            @Override
            public String eventType() {
                return "test01";
            }
        };
        inlet = InletDI.newBuilder().withDatabase(DataBaseProvider.database())
                .withReceivers(List.of(receiver, WantEvent))
                .build();


        new AddSubscriptionsImpl().add(new Subscription(SpsEventType.add_subscriber_01.toString(),
                "addSubscriberUrl", SpsSubscriberType.add_subscriber.toString(), Map.of()));

        publish = baseBuilder()
                .withRetryPolicies(new RetryPolicies(List.of(),
                        List.of()))
                .build();


    }

    static PublishDI.Builder baseBuilder() {
        return PublishDI.newBuilder().withDatabase(DataBaseProvider.configure(EmbeddedDatabase.get()))
                .withClient(new SameJVMClient(new JvmLocalPostImpl(List.of(inlet))));
    }

    @Test
    void add_sub_isAck() {
        AddSubscriberSpsEvent addSubscriberSpsEvent = new AddSubscriberSpsEvent("test01",
                Map.of(),
                "turnUrl",
                "newsub01");

        publish.publish(SpsEventType.add_subscriber_01.toString(), List.of(addSubscriberSpsEvent));
        await().until(() ->
                DataBaseProvider.database().isAck(addSubscriberSpsEvent.id(),
                        SpsSubscriberType.add_subscriber.toString()));
    }

    @Test
    void added_sub_isAck_and_sending_to_that_sub_works() {
        AddSubscriberSpsEvent addSubscriberSpsEvent = new AddSubscriberSpsEvent("test01",
                Map.of(),
                "turnUrl",
                "newsub01");

        publish.publish(SpsEventType.add_subscriber_01.toString(), List.of(addSubscriberSpsEvent));
        await().until(() ->
                DataBaseProvider.database().isAck(addSubscriberSpsEvent.id(),
                        SpsSubscriberType.add_subscriber.toString()));

        publish.publish("test01", List.of(new BasicSpsEvents.BasicSpsEvent("test01", "999",
                Map.of("key", "data"))));

        await().until(() -> receivedEvent != null);
    }
}
