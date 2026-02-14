# ╔═══════════════════════════════════════════════════════════════════════════╗
# ║                         SPS Demo - Makefile                               ║
# ║                     Simple Pub/Sub Demonstration                          ║
# ╚═══════════════════════════════════════════════════════════════════════════╝
#
# Quick start:
#   make build    → Build jar + Docker image
#   make up       → Start all containers
#   make demo     → Send a test event
#   make logs     → Follow container logs
#   make down     → Stop everything
#
# Development:
#   make dev      → Run locally with hot-reload
#   make test     → Run tests
#   make clean    → Clean build artifacts

.PHONY: help build package image up down logs demo dev test clean ps

# ─────────────────────────────────────────────────────────────────────────────
# Configuration
# ─────────────────────────────────────────────────────────────────────────────
IMAGE_NAME := sps-demo
IMAGE_TAG  := latest
COMPOSE    := docker compose -f docker/compose.yml
MVN        := mvn

# Colors for pretty output
CYAN  := \033[36m
GREEN := \033[32m
RESET := \033[0m

# ─────────────────────────────────────────────────────────────────────────────
# Default target
# ─────────────────────────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "$(CYAN)╔═══════════════════════════════════════════════════════════╗$(RESET)"
	@echo "$(CYAN)║              SPS Demo - Available Commands                ║$(RESET)"
	@echo "$(CYAN)╚═══════════════════════════════════════════════════════════╝$(RESET)"
	@echo ""
	@echo "  $(GREEN)Build & Deploy:$(RESET)"
	@echo "    make build     Build everything (jar + image)"
	@echo "    make package   Build jar only"
	@echo "    make image     Build Docker image only"
	@echo ""
	@echo "  $(GREEN)Run:$(RESET)"
	@echo "    make up        Start all containers"
	@echo "    make down      Stop all containers"
	@echo "    make restart   Restart all containers"
	@echo "    make logs      Follow container logs"
	@echo "    make ps        Show running containers"
	@echo ""
	@echo "  $(GREEN)Demo:$(RESET)"
	@echo "    make demo      Send a test event"
	@echo "    make demo-loop Send events in a loop"
	@echo ""
	@echo "  $(GREEN)Development:$(RESET)"
	@echo "    make dev       Run with Quarkus dev mode"
	@echo "    make test      Run tests"
	@echo "    make clean     Clean build artifacts"
	@echo ""

# ─────────────────────────────────────────────────────────────────────────────
# Build targets
# ─────────────────────────────────────────────────────────────────────────────
build: package image
	@echo "$(GREEN)✓ Build complete$(RESET)"

package:
	@echo "$(CYAN)► Building JAR...$(RESET)"
	@cd .. && $(MVN) package -pl sps-demo -am -DskipTests -q
	@echo "$(GREEN)✓ JAR built$(RESET)"

image:
	@echo "$(CYAN)► Building Docker image...$(RESET)"
	@docker build -t $(IMAGE_NAME):$(IMAGE_TAG) .
	@echo "$(GREEN)✓ Image built: $(IMAGE_NAME):$(IMAGE_TAG)$(RESET)"

# ─────────────────────────────────────────────────────────────────────────────
# Container management
# ─────────────────────────────────────────────────────────────────────────────
up:
	@echo "$(CYAN)► Starting containers...$(RESET)"
	@$(COMPOSE) up -d
	@echo "$(GREEN)✓ Containers started$(RESET)"
	@echo ""
	@echo "  Publisher:  http://localhost:8081/demo/publish"
	@echo "  Subscriber: http://localhost:8082/demo/health"
	@echo "  Subscribe:  http://localhost:8083/demo/health"
	@echo ""

down:
	@echo "$(CYAN)► Stopping containers...$(RESET)"
	@$(COMPOSE) down
	@echo "$(GREEN)✓ Containers stopped$(RESET)"

restart: down up

logs:
	@$(COMPOSE) logs -f

ps:
	@$(COMPOSE) ps

# ─────────────────────────────────────────────────────────────────────────────
# Demo targets
# ─────────────────────────────────────────────────────────────────────────────
demo:
	@echo "$(CYAN)► Sending test event...$(RESET)"
	@curl -s -X POST "http://localhost:8081/demo/publish?type=greeting_01" \
		-H "Content-Type: application/json" \
		-d '{"message": "Hello from SPS!", "timestamp": "'$$(date -Iseconds)'"}' | jq .
	@echo ""
	@echo "$(GREEN)✓ Check subscriber logs: make logs$(RESET)"

demo-loop:
	@echo "$(CYAN)► Sending events in a loop (Ctrl+C to stop)...$(RESET)"
	@i=1; while true; do \
		curl -s -X POST "http://localhost:8081/demo/publish?type=demo_event_01" \
			-H "Content-Type: application/json" \
			-d '{"counter": '$$i', "timestamp": "'$$(date -Iseconds)'"}' | jq -c .; \
		i=$$((i+1)); \
		sleep 1; \
	done

# ─────────────────────────────────────────────────────────────────────────────
# Development targets
# ─────────────────────────────────────────────────────────────────────────────
dev:
	@echo "$(CYAN)► Starting Quarkus dev mode...$(RESET)"
	@cd .. && $(MVN) quarkus:dev -pl sps-demo

test:
	@echo "$(CYAN)► Running tests...$(RESET)"
	@cd .. && $(MVN) test -pl sps-demo

clean:
	@echo "$(CYAN)► Cleaning...$(RESET)"
	@cd .. && $(MVN) clean -pl sps-demo
	@$(COMPOSE) down -v 2>/dev/null || true
	@echo "$(GREEN)✓ Clean complete$(RESET)"
