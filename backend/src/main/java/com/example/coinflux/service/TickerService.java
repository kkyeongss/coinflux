package com.example.coinflux.service;

import com.example.coinflux.client.UpbitWebSocketClient;
import com.example.coinflux.domain.Ticker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class TickerService {

    private final UpbitWebSocketClient upbitWebSocketClient;

    public Flux<Ticker> streamAll() {
        return upbitWebSocketClient.stream();
    }

    public Flux<Ticker> streamByCode(String code) {
        return upbitWebSocketClient.stream()
                .filter(ticker -> ticker.getCode().equalsIgnoreCase(code));
    }
}
