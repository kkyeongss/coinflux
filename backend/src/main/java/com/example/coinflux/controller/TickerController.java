package com.example.coinflux.controller;

import com.example.coinflux.domain.Ticker;
import com.example.coinflux.service.TickerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TickerController {

    private final TickerService tickerService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Ticker>> streamAll() {
        return tickerService.streamAll()
                .map(ticker -> ServerSentEvent.<Ticker>builder()
                        .event("ticker")
                        .data(ticker)
                        .build());
    }

    @GetMapping(value = "/stream/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Ticker>> streamByCode(@RequestParam String code) {
        return tickerService.streamByCode(code)
                .map(ticker -> ServerSentEvent.<Ticker>builder()
                        .event("ticker")
                        .data(ticker)
                        .build());
    }
}
