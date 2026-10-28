.PHONY: help build test up down logs ps smoke reset clean

help:
	@echo "make up       - build and start the full stack (http://localhost:8088)"
	@echo "make down     - stop the stack"
	@echo "make logs     - follow logs"
	@echo "make ps       - show container status"
	@echo "make build    - build backend jar and frontend dist locally"
	@echo "make test     - run backend unit/integration tests"
	@echo "make smoke    - run API smoke test against the running stack"
	@echo "make reset    - stop the stack and delete the MySQL volume"

build:
	mvn -B -DskipTests package
	cd web && npm ci --no-audit --no-fund && npm run build

test:
	mvn -B test

up:
	docker compose up -d --build

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

ps:
	docker compose ps

smoke:
	python3 scripts/api-smoke.py --base-url http://127.0.0.1:$${APP_PORT:-8088}

reset:
	docker compose down -v
	docker compose up -d --build

clean:
	mvn -B clean
	rm -rf web/dist
