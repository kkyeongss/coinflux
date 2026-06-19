package com.example.coinflux.controller;

import com.example.coinflux.client.UpbitWebSocketClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final UpbitWebSocketClient upbitClient;

    @GetMapping
    public Mono<Set<String>> getSubscriptions() {
        return Mono.just(upbitClient.getCodes());
    }

    @PostMapping("/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> addSubscription(@PathVariable String code) {
        String normalized = code.startsWith("KRW-") ? code : "KRW-" + code;
        upbitClient.addCode(normalized);
        return Mono.just(ResponseEntity.ok(Map.of("code", normalized, "subscribed", true)));
    }

    @DeleteMapping("/{code}")
    public Mono<ResponseEntity<Map<String, Object>>> removeSubscription(@PathVariable String code) {
        String normalized = code.startsWith("KRW-") ? code : "KRW-" + code;
        upbitClient.removeCode(normalized);
        return Mono.just(ResponseEntity.ok(Map.of("code", normalized, "subscribed", false)));
    }
}
