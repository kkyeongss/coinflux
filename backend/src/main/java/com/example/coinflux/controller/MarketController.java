package com.example.coinflux.controller;

import com.example.coinflux.domain.Market;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@RestController
public class MarketController {

    private final WebClient webClient = WebClient.create("https://api.upbit.com");

    @GetMapping("/api/markets")
    public Flux<Market> getKrwMarkets() {
        return webClient.get()
                .uri("/v1/market/all")
                .retrieve()
                .bodyToFlux(Market.class)
                .filter(m -> m.getMarket().startsWith("KRW-"));
    }
}
