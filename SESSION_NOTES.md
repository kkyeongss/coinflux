# coinflux — 프로젝트 킥오프 노트

> WebFlux 학습용 실시간 암호화폐 시세 대시보드 토이프로젝트
> 이 문서는 기획 세션 대화 내용을 정리한 핸드오프 노트입니다. 터미널 작업 시작점.

---

## 1. 프로젝트 개요

- **이름**: coinflux (코인 + Flux)
- **목적**: Spring WebFlux 학습. reactive 스트리밍의 핵심 패턴을 끝까지 한 바퀴 돌아보기
- **부가 목표**: 실사용 가능 + 포트폴리오에 보여줄 만한 결과물
- **데이터 소스**: Upbit WebSocket 공개 API (무료, 가입/키 불필요, 실시간)

## 2. 왜 암호화폐인가 (의사결정 과정 요약)

여러 후보를 검토한 끝에 암호화폐로 결정:

- **버스/지하철 실시간**: 데이터는 좋지만 "어차피 제때 안 옴" → 탈락
- **환율**: 친숙하고 실생활 밀착형이지만, 무료 티어는 일/시간 단위 갱신이라 실시간 스트림 체감이 약함
- **VIX·시장지표**: FRED API로 가져올 수 있으나 무료는 일별 종가뿐. 실시간 아님
- **비행기 추적(OpenSky)·ISS 위치**: 실시간 맛은 좋지만 실생활 활용도 약함
- **암호화폐 (최종 선택)**: 무료 + 실시간(WebSocket) + 데이터가 항상 살아있어 디버깅 편함. WebFlux 학습엔 최적

> 참고: WebFlux 학습 관점에서는 데이터 갱신 주기가 핵심이 아님.
> "주기적으로 가져오기 → Flux 스트림 변환 → 여러 클라이언트에 푸시" 패턴은 소스가 뭐든 동일하게 연습됨.
> 다만 실시간 데이터가 시각적 피드백과 디버깅에 유리해서 암호화폐로 결정.

## 3. 기술 스택

- **백엔드**: Spring Boot + Spring WebFlux
- **외부 연동**: ReactorNettyWebSocketClient (Upbit WebSocket 구독)
- **프론트**: React + Vite (EventSource로 SSE 수신)
- **(선택) 영속화**: R2DBC + H2 — 알림 규칙/가격 히스토리 저장 시
- 개발 환경: Mac + JetBrains

## 4. 핵심 데이터 흐름

```
Upbit WebSocket   →   하나의 연결로 시세 구독
        ↓
Flux<Ticker>      (서버에서 가공·필터)
        ↓
Sinks.Many        (멀티캐스트 허브) → 여러 브라우저가 공유 구독
        ↓
Flux<ServerSentEvent>  →  React에 SSE 푸시
```

## 5. WebFlux 핵심 학습 포인트

1. **외부 WebSocket을 reactive하게 구독** — `ReactorNettyWebSocketClient`
2. **`Sinks.Many`로 멀티캐스트** — Upbit 연결은 하나만 맺고 여러 클라이언트에 분배.
   "왜 클라이언트마다 외부 연결을 새로 맺으면 안 되는가"를 체감하는 부분
3. **SSE 스트리밍** — `Flux<ServerSentEvent>`로 브라우저에 푸시

## 6. 단계별 로드맵

- [ ] **1단계**: WebFlux 프로젝트 생성 + 코인 1개(BTC) 시세를 SSE로 푸시 — 최소 동작 버전
- [ ] **2단계**: `Sinks.Many` 도입, 멀티 클라이언트 공유 구독으로 리팩터링
- [ ] **3단계**: React에서 `EventSource`로 받아 실시간 가격 표시
- [ ] **4단계**: 여러 코인 구독 + 프론트 선택 UI
- [ ] **5단계**: 가격 알림 규칙 (목표가 도달 시 별도 알림 채널)
- [ ] **6단계**: (선택) R2DBC로 알림 규칙 영속화 + 가격 히스토리 차트

> 1단계 "BTC 하나를 SSE로 푸시"까지만 가도 WebFlux 본질엔 거의 닿음. 거기서부터 붙여나가기.

## 7. 패키지 구조 (예시)

```
com.example.coinflux
├── client/
│   └── UpbitWebSocketClient    // 외부 WebSocket 구독, Flux 발행
├── service/
│   ├── TickerService           // Sinks.Many 허브, 멀티캐스트
│   └── AlertService            // 가격 조건 감시 + 알림
├── controller/
│   └── TickerController        // GET /api/stream (SSE 엔드포인트)
├── domain/
│   └── Ticker, AlertRule
└── config/
    └── WebFluxConfig, CorsConfig
```

## 8. 데이터 소스 메모 (Upbit WebSocket)

- 공개 WebSocket, 가입·인증키 불필요
- 구독 시 JSON으로 ticket / type(예: `ticker`) / codes(예: `["KRW-BTC"]`) 전송
- 응답으로 실시간 체결가(trade_price) 등 수신
- 1단계에서는 `KRW-BTC` 하나만 구독해서 흐름 확인

## 9. 다음 작업

터미널로 진행. 다음 중 하나부터:
- 프로젝트 세팅 (build.gradle + 의존성 + 기본 구조)
- 1단계 동작 코드 (BTC 시세 → SSE 푸시)
- Upbit WebSocket API 명세 정리
