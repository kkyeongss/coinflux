package com.example.coinflux.service;

import com.example.coinflux.domain.AlertEvent;
import com.example.coinflux.domain.AlertRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final TickerService tickerService;

    private final Map<String, AlertRule> rules = new ConcurrentHashMap<>();
    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();
    private final Sinks.Many<AlertEvent> alertSink = Sinks.many().multicast().onBackpressureBuffer();

    public AlertRule addRule(AlertRule request) {
        AlertRule rule = new AlertRule(
                UUID.randomUUID().toString(),
                request.getCode(),
                request.getCondition(),
                request.getTargetPrice(),
                System.currentTimeMillis()
        );
        rules.put(rule.getId(), rule);

        // 핵심 WebFlux 패턴: 조건 달성 시 한 번만 발화하고 자동 구독 해제
        Disposable sub = tickerService.streamByCode(rule.getCode())
                .filter(ticker -> isTriggered(rule, ticker.getTradePrice()))
                .take(1)
                .subscribe(ticker -> {
                    log.info("Alert fired! rule={} code={} price={}", rule.getId(), rule.getCode(), ticker.getTradePrice());
                    rules.remove(rule.getId());
                    subscriptions.remove(rule.getId());
                    alertSink.tryEmitNext(new AlertEvent(
                            rule.getId(),
                            rule.getCode(),
                            rule.getCondition(),
                            rule.getTargetPrice(),
                            ticker.getTradePrice(),
                            System.currentTimeMillis()
                    ));
                });

        subscriptions.put(rule.getId(), sub);
        return rule;
    }

    public boolean removeRule(String id) {
        Disposable sub = subscriptions.remove(id);
        if (sub != null) sub.dispose();
        return rules.remove(id) != null;
    }

    public List<AlertRule> listRules() {
        return List.copyOf(rules.values());
    }

    public Flux<AlertEvent> streamAlerts() {
        return alertSink.asFlux();
    }

    private boolean isTriggered(AlertRule rule, double price) {
        return switch (rule.getCondition()) {
            case "ABOVE" -> price >= rule.getTargetPrice();
            case "BELOW" -> price <= rule.getTargetPrice();
            default -> false;
        };
    }
}
