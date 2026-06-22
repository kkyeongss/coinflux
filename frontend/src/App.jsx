import { useEffect, useRef, useState } from 'react'
import './App.css'

const API = import.meta.env.VITE_API_URL ?? ''
const DEFAULT_WATCHLIST = ['KRW-BTC', 'KRW-ETH', 'KRW-XRP', 'KRW-SOL', 'KRW-DOGE', 'KRW-ADA', 'KRW-AVAX', 'KRW-DOT']
const LS_WATCHLIST = 'coinflux-watchlist'
const LS_SELECTED  = 'coinflux-selected'

function loadLS(key, fallback) {
  try { return JSON.parse(localStorage.getItem(key)) ?? fallback } catch { return fallback }
}

function formatPrice(price) {
  return Math.round(price).toLocaleString('ko-KR')
}
function formatVolume(vol) {
  if (!vol) return '-'
  if (vol >= 1_000_000) return `${(vol / 1_000_000).toFixed(2)}M`
  if (vol >= 1000) return `${(vol / 1000).toFixed(2)}K`
  return vol.toFixed(2)
}

/* ── CoinSearch ── */
function CoinSearch({ markets, watchlist, onAdd, onClose }) {
  const [query, setQuery] = useState('')
  const inputRef = useRef(null)

  useEffect(() => inputRef.current?.focus(), [])

  const results = query.trim().length === 0 ? [] : markets
    .filter((m) => !watchlist.has(m.market))
    .filter((m) =>
      m.market.toLowerCase().includes(query.toLowerCase()) ||
      m.english_name.toLowerCase().includes(query.toLowerCase()) ||
      m.korean_name.includes(query)
    )
    .slice(0, 8)

  return (
    <div className="search-wrap">
      <input
        ref={inputRef}
        className="search-input"
        placeholder="코인 검색 (BTC, 비트코인, Solana…)"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        onKeyDown={(e) => e.key === 'Escape' && onClose()}
      />
      {results.length > 0 && (
        <ul className="search-results">
          {results.map((m) => (
            <li key={m.market} className="search-item" onClick={() => { onAdd(m.market); setQuery('') }}>
              <span className="search-symbol">{m.market.replace('KRW-', '')}</span>
              <span className="search-name">{m.korean_name}</span>
              <span className="search-en">{m.english_name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

/* ── Toast ── */
function ToastList({ toasts, onDismiss }) {
  return (
    <div className="toast-list">
      {toasts.map((t) => (
        <div key={t.id} className="toast" onClick={() => onDismiss(t.id)}>
          <div className="toast-title">🔔 {t.code.replace('KRW-', '')} 알림 도달</div>
          <div className="toast-body">
            목표 ₩{formatPrice(t.targetPrice)} ({t.condition === 'ABOVE' ? '이상' : '이하'})<br />
            현재 ₩{formatPrice(t.actualPrice)}
          </div>
        </div>
      ))}
    </div>
  )
}

/* ── AlertPanel ── */
function AlertPanel({ tickers, watchlist, rules, onAdd, onRemove }) {
  const [code, setCode] = useState('KRW-BTC')
  const [condition, setCondition] = useState('ABOVE')
  const [targetPrice, setTargetPrice] = useState('')

  function handleSubmit(e) {
    e.preventDefault()
    const price = Number(targetPrice.replace(/,/g, '')) || currentPrice
    if (!price || price <= 0) return
    onAdd({ code, condition, targetPrice: price })
    setTargetPrice('')
  }

  const currentPrice = tickers[code]?.tradePrice

  return (
    <div className="alert-panel">
      <h2 className="alert-title">가격 알림</h2>
      <form className="alert-form" onSubmit={handleSubmit}>
        <select value={code} onChange={(e) => setCode(e.target.value)}>
          {[...watchlist].map((c) => (
            <option key={c} value={c}>{c.replace('KRW-', '')}</option>
          ))}
        </select>
        <select value={condition} onChange={(e) => setCondition(e.target.value)}>
          <option value="ABOVE">이상 도달 시</option>
          <option value="BELOW">이하 도달 시</option>
        </select>
        <div className="price-input-wrap">
          <span className="currency">₩</span>
          <input
            type="text"
            placeholder={currentPrice ? formatPrice(currentPrice) : '목표가 입력'}
            value={targetPrice}
            onChange={(e) => setTargetPrice(e.target.value)}
          />
        </div>
        <button type="submit">추가</button>
      </form>

      {rules.length > 0 ? (
        <ul className="rule-list">
          {rules.map((r) => (
            <li key={r.id} className="rule-item">
              <span className="rule-desc">
                <strong>{r.code.replace('KRW-', '')}</strong>
                {' '}₩{formatPrice(r.targetPrice)}{' '}
                <em>{r.condition === 'ABOVE' ? '이상' : '이하'}</em>
              </span>
              <button className="rule-remove" onClick={() => onRemove(r.id)}>✕</button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="no-rules">등록된 알림이 없습니다.</p>
      )}
    </div>
  )
}

/* ── TickerCard ── */
function TickerCard({ code, ticker, koreanName, onRemove }) {
  const symbol = code.replace('KRW-', '')
  const change = ticker?.change ?? 'EVEN'
  const changeClass = change === 'RISE' ? 'rise' : change === 'FALL' ? 'fall' : 'even'

  return (
    <div className={`card ${ticker ? changeClass : 'loading'}`}>
      <div className="card-header">
        <span className="symbol">{symbol}</span>
        <span className="coin-name">{koreanName ?? symbol}</span>
        <button className="card-remove" onClick={onRemove} title="목록에서 제거">✕</button>
        <span className="live-dot" />
      </div>
      {ticker ? (
        <>
          <div className="price">₩{formatPrice(ticker.tradePrice)}</div>
          <div className="change-row">
            <span className={`badge ${changeClass}`}>
              {change === 'RISE' ? '▲' : change === 'FALL' ? '▼' : '—'}{' '}
              {(ticker.changeRate * 100).toFixed(2)}%
            </span>
          </div>
          <div className="volume">
            24h {formatVolume(ticker.accTradeVolume24h)} {symbol}
          </div>
          <div className="updated">
            {new Date(ticker.timestamp).toLocaleTimeString('ko-KR')} 기준
          </div>
        </>
      ) : (
        <div className="placeholder">수신 대기 중...</div>
      )}
    </div>
  )
}

/* ── App ── */
export default function App() {
  const [tickers, setTickers]   = useState({})
  const [status, setStatus]     = useState('connecting')
  const [markets, setMarkets]   = useState([])              // Upbit 전체 KRW 마켓
  const [watchlist, setWatchlist] = useState(              // 사용자 등록 코인
    () => new Set(loadLS(LS_WATCHLIST, DEFAULT_WATCHLIST))
  )
  const [selected, setSelected] = useState(               // 현재 표시 중인 코인
    () => new Set(loadLS(LS_SELECTED, DEFAULT_WATCHLIST))
  )
  const [rules, setRules]       = useState([])
  const [toasts, setToasts]     = useState([])
  const [searchOpen, setSearchOpen] = useState(false)
  const toastTimer = useRef({})

  // 브라우저 알림 권한 요청
  useEffect(() => {
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
  }, [])

  // Upbit 마켓 목록 로드 (한 번)
  useEffect(() => {
    fetch(`${API}/api/markets`)
      .then((r) => r.json())
      .then(setMarkets)
      .catch(() => {})
  }, [])

  // 시세 SSE
  useEffect(() => {
    const es = new EventSource(`${API}/api/stream`)
    es.addEventListener('ticker', (e) => {
      const data = JSON.parse(e.data)
      setTickers((prev) => ({ ...prev, [data.code]: data }))
      setStatus('connected')
    })
    es.onerror = () => setStatus('error')
    return () => es.close()
  }, [])

  // 알림 SSE
  useEffect(() => {
    const es = new EventSource(`${API}/api/alerts/stream`)
    es.addEventListener('alert', (e) => {
      const event = JSON.parse(e.data)
      const toast = { ...event, id: Date.now() }
      setToasts((prev) => [...prev, toast])
      toastTimer.current[toast.id] = setTimeout(() => dismissToast(toast.id), 5000)
      setRules((prev) => prev.filter((r) => r.id !== event.ruleId))

      if (Notification.permission === 'granted') {
        new Notification(`${event.code.replace('KRW-', '')} 목표가 도달`, {
          body: `₩${formatPrice(event.actualPrice)} (목표 ${event.condition === 'ABOVE' ? '이상' : '이하'} ₩${formatPrice(event.targetPrice)})`,
          icon: '/favicon.ico',
        })
      }
    })
    return () => es.close()
  }, [])

  // localStorage 동기화
  useEffect(() => {
    localStorage.setItem(LS_WATCHLIST, JSON.stringify([...watchlist]))
  }, [watchlist])
  useEffect(() => {
    localStorage.setItem(LS_SELECTED, JSON.stringify([...selected]))
  }, [selected])

  function dismissToast(id) {
    clearTimeout(toastTimer.current[id])
    delete toastTimer.current[id]
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }

  function toggleSelected(code) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(code)) {
        if (next.size === 1) return prev
        next.delete(code)
      } else {
        next.add(code)
      }
      return next
    })
  }

  async function addToWatchlist(code) {
    setWatchlist((prev) => new Set([...prev, code]))
    setSelected((prev) => new Set([...prev, code]))
    setSearchOpen(false)
    await fetch(`${API}/api/subscriptions/${code}`, { method: 'POST' })
  }

  async function removeFromWatchlist(code) {
    setWatchlist((prev) => { const n = new Set(prev); n.delete(code); return n })
    setSelected((prev)  => { const n = new Set(prev); n.delete(code); return n })
    await fetch(`${API}/api/subscriptions/${code}`, { method: 'DELETE' })
  }

  async function addRule(payload) {
    const res = await fetch(`${API}/api/alerts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
    const rule = await res.json()
    setRules((prev) => [...prev, rule])
  }

  async function removeRule(id) {
    await fetch(`${API}/api/alerts/${id}`, { method: 'DELETE' })
    setRules((prev) => prev.filter((r) => r.id !== id))
  }

  const marketMap = Object.fromEntries(markets.map((m) => [m.market, m]))
  const visibleCoins = [...watchlist].filter((c) => selected.has(c))

  return (
    <div className="app">
      <header className="app-header">
        <h1 className="logo">CoinFlux</h1>
        <span className={`status-badge ${status}`}>
          {status === 'connected' ? '● LIVE' : status === 'error' ? '● 연결 오류' : '○ 연결 중'}
        </span>
      </header>

      {/* 코인 선택 + 검색 */}
      <div className="selector-row">
        <div className="selector">
          {[...watchlist].map((code) => (
            <button
              key={code}
              className={`coin-btn ${selected.has(code) ? 'active' : ''}`}
              onClick={() => toggleSelected(code)}
            >
              {code.replace('KRW-', '')}
            </button>
          ))}
          <button
            className={`coin-btn add-btn ${searchOpen ? 'active' : ''}`}
            onClick={() => setSearchOpen((v) => !v)}
          >
            + 추가
          </button>
        </div>

        {searchOpen && (
          <CoinSearch
            markets={markets}
            watchlist={watchlist}
            onAdd={addToWatchlist}
            onClose={() => setSearchOpen(false)}
          />
        )}
      </div>

      <main className="grid">
        {visibleCoins.map((code) => (
          <TickerCard
            key={code}
            code={code}
            ticker={tickers[code]}
            koreanName={marketMap[code]?.korean_name}
            onRemove={() => removeFromWatchlist(code)}
          />
        ))}
      </main>

      <AlertPanel
        tickers={tickers}
        watchlist={watchlist}
        rules={rules}
        onAdd={addRule}
        onRemove={removeRule}
      />

      <ToastList toasts={toasts} onDismiss={dismissToast} />
    </div>
  )
}
