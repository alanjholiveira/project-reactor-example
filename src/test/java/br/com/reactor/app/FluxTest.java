package br.com.reactor.app;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

@Slf4j
public class FluxTest {

//    @BeforeAll
//    public static void setUp() {
//        BlockHound.install();
//    }
//
//    @Test
//    public void blockHoundWorks() {
//        try {
//            FutureTask<?> task = new FutureTask<>(() -> {
//                Thread.sleep(0);
//                return "";
//            });
//            Schedulers.parallel().schedule(task);
//
//            task.get(10, TimeUnit.SECONDS);
//            Assertions.fail("should fail");
//        } catch (Exception e) {
//            Assertions.assertTrue(e.getCause() instanceof BlockingOperationError);
//        }
//    }

    @Test
    public void fluxSubscriber() {
        Flux<String> fluxString = Flux.just("Alan", "Oliveira", "WebFlux", "Academy")
                .log();

        StepVerifier.create(fluxString)
                .expectNext("Alan", "Oliveira", "WebFlux", "Academy")
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbers() {
        Flux<Integer> flux = Flux.range(1, 5)
                .log();

        flux.subscribe(i -> log.info("Number {}", i));

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3,4,5)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberFromList() {
        Flux<Integer> flux = Flux.fromIterable(List.of(1,2,3,4,5))
                .log();

        flux.subscribe(i -> log.info("Number {}", i));

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3,4,5)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumberError() {
        Flux<Integer> flux = Flux.range(1,5)
                .log()
                .map(i -> {
                    if (i == 4) {
                        throw new IndexOutOfBoundsException("index error");
                    }
                    return i;
                });

        flux.subscribe(i -> log.info("Number {}", i), Throwable::printStackTrace,
                () -> log.info("DONE!"), subscription -> subscription.request(3));

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3)
                .expectError(IndexOutOfBoundsException.class)
                .verify();
    }

    @Test
    public void fluxSubscriberNumbersUglyBackpressure() {
        Flux<Integer> flux = Flux.range(1,10)
                .log();

        flux.subscribe(new Subscriber<Integer>() {
            private int count = 0;
            private Subscription subscription;
            private final int requestCount = 2;

            @Override
            public void onSubscribe(Subscription subscription) {
                this.subscription = subscription;
                subscription.request(requestCount);
            }

            @Override
            public void onNext(Integer integer) {
                count++;
                if (count >= requestCount) {
                    count = 0;
                    subscription.request(requestCount);
                }
            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberNumbersNotSoUglyBackpressure() {
        Flux<Integer> flux = Flux.range(1,10)
                .log();

        flux.subscribe(new BaseSubscriber<Integer>() {
            private int count = 0;
            private final int requestCount = 2;

            @Override
            protected void hookOnSubscribe(Subscription subscription) {
                request(requestCount);
            }

            @Override
            protected void hookOnNext(Integer value) {
                count++;
                if (count >= requestCount) {
                    count = 0;
                    request(requestCount);
                }
            }
        });

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberPrettyBackpressure() {
        Flux<Integer> flux = Flux.range(1, 10)
                .log()
                .limitRate(3);

        flux.subscribe(i -> log.info("Number {}", i));

        log.info("-------------------------------------");

        StepVerifier.create(flux)
                .expectNext(1,2,3,4,5,6,7,8,9,10)
                .verifyComplete();
    }

    @Test
    public void fluxSubscriberIntervalOne() throws Exception {
        Flux<Long> interval = Flux.interval(Duration.ofMillis(100))
                .log();

        interval.subscribe(i -> log.info("Number {}", i));

        Thread.sleep(3000);
    }

    @Test
    public void fluxSubscriberIntervalTwo() throws Exception {
       StepVerifier.withVirtualTime(this::createInterval)
               .expectSubscription()
               .expectNoEvent(Duration.ofDays(1))
               .thenAwait(Duration.ofDays(1))
               .expectNext(0L)
               .thenAwait(Duration.ofDays(1))
               .expectNext(1L)
               .thenCancel()
               .verify();
    }

    private Flux<Long> createInterval() {
        return Flux.interval(Duration.ofDays(1))
                .log();
    }

    @Test
    public void connectableFLux() throws Exception {
        ConnectableFlux<Integer> connectableFlux = Flux.range(1, 10)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish();

       /* connectableFlux.connect();

        log.info("Thread sleeping for 300ms");

        Thread.sleep(300);

        connectableFlux.subscribe(i -> log.info("Sub1 number {}", i));

        log.info("Thread sleeping for 200ms");

        Thread.sleep(200);

        connectableFlux.subscribe(i -> log.info("Sub2 number {}", i));*/

        StepVerifier
                .create(connectableFlux)
                .then(connectableFlux::connect)
                .thenConsumeWhile(i -> i <= 5)
                .expectNext(6,7,8,9,10)
                .expectComplete()
                .verify();
    }

    @Test
    public void connectableFLuxAutoConnect() throws Exception {
        Flux<Integer> fluxAutoConnect = Flux.range(1, 5)
                .log()
                .delayElements(Duration.ofMillis(100))
                .publish()
                .autoConnect(2);

        StepVerifier
                .create(fluxAutoConnect)
                .then(fluxAutoConnect::subscribe)
                .expectNext(1,2,3,4,5)
                .expectComplete()
                .verify();
    }

}
