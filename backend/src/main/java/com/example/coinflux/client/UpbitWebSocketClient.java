package com.example.coinflux.client;

import com.example.coinflux.domain.Ticker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketClient {

    private static final String UPBIT_WS_URL = "wss://api.upbit.com/websocket/v1";
    private static final List<String> DEFAULT_CODES = List.of(
            "KRW-BTC", "KRW-ETH", "KRW-XRP",
            "KRW-SOL", "KRW-DOGE", "KRW-ADA", "KRW-AVAX", "KRW-DOT"
    );

    private final ObjectMapper objectMapper;
    private final Sinks.Many<Ticker> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Set<String> codes = ConcurrentHashMap.newKeySet();
    private volatile Disposable connection;

    @PostConstruct
    public void connect() {
        codes.addAll(DEFAULT_CODES);
        reconnect();
    }

    public void addCode(String code) {
        if (codes.add(code)) {
            log.info("Added code: {}, reconnecting...", code);
            reconnect();
        }
    }

    public void removeCode(String code) {
        if (codes.remove(code)) {
            log.info("Removed code: {}, reconnecting...", code);
            reconnect();
        }
    }

    public Set<String> getCodes() {
        return Set.copyOf(codes);
    }

    public Flux<Ticker> stream() {
        return sink.asFlux();
    }

    private synchronized void reconnect() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        connection = startConnection();
    }

    private Disposable startConnection() {
        List<String> snapshot = List.copyOf(codes);
        log.info("Connecting to Upbit with {} codes", snapshot.size());
        var client = new ReactorNettyWebSocketClient();
        return client.execute(URI.create(UPBIT_WS_URL), session -> {
            log.info("WebSocket session established");
            WebSocketMessage request = session.textMessage(buildSubscribeMessage(snapshot));
            return session.send(Flux.just(request))
                    .thenMany(session.receive()
                            .map(msg -> msg.getPayload().toString(StandardCharsets.UTF_8))
                            .doOnNext(raw -> log.debug("Raw: {}", raw.substring(0, Math.min(raw.length(), 80))))
                            .flatMap(this::parseTicker)
                            .doOnNext(t -> sink.tryEmitNext(t))
                    )
                    .then();
        })
        .doOnError(e -> log.error("WebSocket error: {}", e.getMessage()))
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry(s -> log.warn("Reconnecting... attempt {}", s.totalRetries() + 1)))
        .subscribe();
    }

    private Flux<Ticker> parseTicker(String json) {
        try {
            Ticker ticker = objectMapper.readValue(json, Ticker.class);
            if (ticker.getCode() == null) return Flux.empty();
            return Flux.just(ticker);
        } catch (Exception e) {
            log.debug("Skipping non-ticker message");
            return Flux.empty();
        }
    }

    private String buildSubscribeMessage(List<String> codeList) {
        try {
            var payload = List.of(
                    Map.of("ticket", "coinflux"),
                    Map.of("type", "ticker", "codes", codeList)
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build subscribe message", e);
        }
    }
}
