# CoinFlux

> Spring WebFlux 학습용 실시간 암호화폐 시세 대시보드

Upbit WebSocket으로 실시간 시세를 구독하고, SSE(Server-Sent Events)로 브라우저에 푸시하는 WebFlux 풀스택 토이프로젝트입니다.

## 기술 스택

| 영역 | 기술 |
|---|---|
| 백엔드 | Spring Boot 3.5, Spring WebFlux, Reactor Netty |
| 프론트엔드 | React 19, Vite |
| 데이터 소스 | Upbit WebSocket 공개 API (인증 불필요) |
| 빌드 | Gradle 8, npm |

## 핵심 데이터 흐름

```
Upbit WebSocket
      │  (바이너리 프레임 → UTF-8 JSON)
      ▼
UpbitWebSocketClient
      │  Sinks.Many<Ticker>  ← 하나의 연결로 전체 구독
      ▼
TickerService.streamAll()
      │  Flux<Ticker>
      ▼
TickerController  →  GET /api/stream  →  Flux<ServerSentEvent>
      │
      ▼
React EventSource  →  실시간 카드 UI 업데이트
```

## 주요 기능

- **실시간 시세** — BTC, ETH, XRP 등 Upbit KRW 마켓 265개 코인 실시간 스트리밍
- **코인 검색/추가** — 원하는 코인을 검색해 대시보드에 추가 (localStorage 저장)
- **가격 알림** — 목표가 설정 시 조건 달성 시 토스트 알림 (`filter + take(1)` 패턴)
- **동적 구독** — 코인 추가/삭제 시 Upbit WebSocket 자동 재연결

## 실행 방법

```bash
# 백엔드 + 프론트엔드 동시 시작
make dev

# 종료
make stop

# 재시작
make restart

# 로그 확인
make logs
```

| 서버 | 주소 |
|---|---|
| 백엔드 API | http://localhost:8080 |
| 프론트엔드 | http://localhost:5173 |

## API

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/stream` | 전체 코인 실시간 시세 SSE |
| GET | `/api/stream/code?code=KRW-BTC` | 특정 코인 시세 SSE |
| GET | `/api/markets` | Upbit KRW 마켓 전체 목록 |
| GET | `/api/subscriptions` | 현재 구독 중인 코인 목록 |
| POST | `/api/subscriptions/{code}` | 코인 구독 추가 |
| DELETE | `/api/subscriptions/{code}` | 코인 구독 해제 |
| GET | `/api/alerts` | 등록된 알림 규칙 목록 |
| POST | `/api/alerts` | 알림 규칙 등록 |
| DELETE | `/api/alerts/{id}` | 알림 규칙 삭제 |
| GET | `/api/alerts/stream` | 알림 발화 SSE |

## 프로젝트 구조

```
coinflux/
├── backend/
│   └── src/main/java/com/example/coinflux/
│       ├── client/      UpbitWebSocketClient     # Upbit WS 구독, Sinks.Many 발행
│       ├── service/     TickerService             # 스트림 필터링
│       │                AlertService              # 알림 규칙 관리 (filter + take(1))
│       ├── controller/  TickerController          # SSE 엔드포인트
│       │                AlertController           # 알림 CRUD + SSE
│       │                MarketController          # Upbit 마켓 목록 프록시
│       │                SubscriptionController    # 동적 구독 관리
│       ├── domain/      Ticker, AlertRule, AlertEvent, Market
│       └── config/      CorsConfig
├── frontend/
│   └── src/
│       ├── App.jsx      # 메인 컴포넌트 (시세 카드, 검색, 알림 패널)
│       └── App.css
└── Makefile             # 개발 서버 통합 실행
```

## WebFlux 학습 포인트

1. **`ReactorNettyWebSocketClient`** — 외부 WebSocket을 reactive하게 구독
2. **`Sinks.Many` 멀티캐스트** — Upbit 연결 하나로 여러 SSE 클라이언트에 분배
3. **`Flux<ServerSentEvent>`** — 브라우저에 SSE 스트림 푸시
4. **`filter + take(1)`** — 조건 달성 시 one-shot 발화 후 자동 구독 해제
