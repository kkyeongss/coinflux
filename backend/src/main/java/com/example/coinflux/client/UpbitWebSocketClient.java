package com.example.coinflux.client;

import com.example.coinflux.domain.Ticker;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpbitWebSocketClient {

    private static final String UPBIT_WS_URL = "wss://api.upbit.com/websocket/v1";
    private static final List<String> CODES = List.of("KRW-BTC", "KRW-ETH", "KRW-XRP");

    private final ObjectMapper objectMapper;
    private final Sinks.Many<Ticker> sink = Sinks.many().multicast().onBackpressureBuffer();

    @PostConstruct
    public void connect() {
        connectWithRetry();
    }

    private void connectWithRetry() {
        var client = new ReactorNettyWebSocketClient();
        client.execute(URI.create(UPBIT_WS_URL), session -> {
            String subscribeMsg = buildSubscribeMessage();
            WebSocketMessage request = session.textMessage(subscribeMsg);

            return session.send(Flux.just(request))
                    .thenMany(session.receive()
                            .map(WebSocketMessage::getPayloadAsText)
                            .flatMap(this::parseTicker)
                            .doOnNext(ticker -> sink.tryEmitNext(ticker))
                    )
                    .then();
        })
        .doOnError(e -> log.error("WebSocket error: {}", e.getMessage()))
        .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                .maxBackoff(Duration.ofSeconds(30))
                .doBeforeRetry(signal -> log.warn("Reconnecting to Upbit... attempt {}", signal.totalRetries() + 1)))
        .subscribe();
    }

    private Flux<Ticker> parseTicker(String json) {
        try {
            Ticker ticker = objectMapper.readValue(json, Ticker.class);
            // Upbit sends PING frames and other non-ticker messages; filter them
            if (ticker.getCode() == null) return Flux.empty();
            return Flux.just(ticker);
        } catch (Exception e) {
            log.debug("Skipping non-ticker message: {}", json.substring(0, Math.min(json.length(), 50)));
            return Flux.empty();
        }
    }

    public Flux<Ticker> stream() {
        return sink.asFlux();
    }

    private String buildSubscribeMessage() {
        try {
            var payload = List.of(
                    Map.of("ticket", "coinflux"),
                    Map.of("type", "ticker", "codes", CODES)
            );
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build subscribe message", e);
        }
    }
}
