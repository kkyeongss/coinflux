# CoinFlux 백엔드

Spring Boot 3.5 + Spring WebFlux 기반 실시간 시세 스트리밍 서버입니다.

## 실행

```bash
./gradlew bootRun
```

## 의존성

| 의존성 | 용도 |
|---|---|
| `spring-boot-starter-webflux` | WebFlux, Reactor, Netty 내장 서버 |
| `lombok` | 보일러플레이트 코드 제거 |

## 주요 클래스

### `UpbitWebSocketClient`
- `ReactorNettyWebSocketClient`로 Upbit WebSocket 연결 유지
- 수신 데이터(바이너리 프레임 → UTF-8)를 `Sinks.Many<Ticker>`로 멀티캐스트
- 코인 추가/삭제 시 자동 재연결 (`synchronized reconnect()`)
- 연결 끊김 시 지수 백오프로 자동 재시도

### `TickerService`
- `streamAll()` — 전체 코인 스트림
- `streamByCode(code)` — 특정 코인 필터 스트림

### `AlertService`
- 알림 규칙 등록 시 `tickerStream.filter().take(1).subscribe()` 패턴으로 구독
- 조건 달성 시 one-shot 발화 후 자동 구독 해제, `Disposable` 관리

## 환경 설정 (`application.properties`)

```properties
server.port=8080
logging.level.com.example.coinflux=DEBUG
```

## Upbit WebSocket 메모

- 엔드포인트: `wss://api.upbit.com/websocket/v1`
- 인증 불필요 (공개 API)
- 응답 포맷: 바이너리 프레임 (UTF-8 인코딩 JSON)
- 구독 메시지: `[{"ticket":"..."}, {"type":"ticker","codes":["KRW-BTC",...]}]`
