package com.example.coinflux.controller;

import com.example.coinflux.domain.AlertEvent;
import com.example.coinflux.domain.AlertRule;
import com.example.coinflux.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping
    public Mono<AlertRule> addRule(@RequestBody AlertRule request) {
        return Mono.just(alertService.addRule(request));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> removeRule(@PathVariable String id) {
        boolean removed = alertService.removeRule(id);
        return Mono.just(removed
                ? ResponseEntity.noContent().<Void>build()
                : ResponseEntity.notFound().<Void>build());
    }

    @GetMapping
    public Mono<List<AlertRule>> listRules() {
        return Mono.just(alertService.listRules());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AlertEvent>> streamAlerts() {
        return alertService.streamAlerts()
                .map(event -> ServerSentEvent.<AlertEvent>builder()
                        .event("alert")
                        .data(event)
                        .build());
    }
}
