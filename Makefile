.PHONY: dev stop restart logs

dev:
	@echo "Starting backend..."
	@cd backend && ./gradlew bootRun > /tmp/coinflux-backend.log 2>&1 & echo $$! > /tmp/coinflux-backend.pid
	@echo "Waiting for backend..."
	@until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 || grep -q "Started CoinfluxApplication" /tmp/coinflux-backend.log 2>/dev/null; do sleep 1; done
	@echo "Backend ready."
	@echo "Starting frontend..."
	@cd frontend && npm run dev > /tmp/coinflux-frontend.log 2>&1 & echo $$! > /tmp/coinflux-frontend.pid
	@sleep 3
	@echo ""
	@echo "  Backend : http://localhost:8080"
	@grep "Local:" /tmp/coinflux-frontend.log | tail -1 | sed 's/.*➜/  Frontend:/'
	@echo ""

stop:
	@[ -f /tmp/coinflux-backend.pid ] && kill $$(cat /tmp/coinflux-backend.pid) 2>/dev/null && rm /tmp/coinflux-backend.pid && echo "Backend stopped" || true
	@[ -f /tmp/coinflux-frontend.pid ] && kill $$(cat /tmp/coinflux-frontend.pid) 2>/dev/null && rm /tmp/coinflux-frontend.pid && echo "Frontend stopped" || true
	@pkill -f "coinflux" 2>/dev/null || true
	@lsof -ti :8080 | xargs kill -9 2>/dev/null || true

restart: stop
	@sleep 1
	@$(MAKE) dev

logs:
	@echo "=== Backend ===" && tail -20 /tmp/coinflux-backend.log
	@echo "=== Frontend ===" && tail -10 /tmp/coinflux-frontend.log
