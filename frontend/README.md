# CoinFlux 프론트엔드

React + Vite 기반 실시간 암호화폐 시세 대시보드입니다.

## 실행

```bash
npm install
npm run dev    # http://localhost:5173
npm run build  # 프로덕션 빌드
```

## 주요 구성

| 파일 | 역할 |
|---|---|
| `src/App.jsx` | 전체 UI — 시세 카드, 코인 검색, 알림 패널, 토스트 |
| `src/App.css` | 컴포넌트 스타일 (라이트/다크 모드 자동 지원) |
| `src/index.css` | 전역 CSS 변수 및 기본 스타일 |

## 백엔드 연동

- **시세 SSE** — `EventSource('http://localhost:8080/api/stream')`
- **알림 SSE** — `EventSource('http://localhost:8080/api/alerts/stream')`
- **코인 목록** — `GET /api/markets` (Upbit KRW 마켓 265개)
- **구독 관리** — `POST/DELETE /api/subscriptions/{code}`

## 사용자 데이터 저장 (localStorage)

| 키 | 내용 |
|---|---|
| `coinflux-watchlist` | 사용자가 추가한 코인 목록 |
| `coinflux-selected` | 현재 화면에 표시 중인 코인 |
