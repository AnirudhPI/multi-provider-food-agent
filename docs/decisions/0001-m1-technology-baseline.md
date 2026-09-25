# ADR 0001: M1 Technology Baseline

- **Status:** Accepted
- **Date:** 2026-09-25

## Context

M1 needs a minimal backend that proves a local model can be called through a replaceable boundary and can return validated structured data. The system must remain understandable, testable, and suitable for later MCP integrations.

## Decision

- Use Java 21 installed through Homebrew.
- Use Maven and commit the Maven Wrapper.
- Use Spring Boot 4.1.1 with Spring MVC.
- Use a layered Maven modular monolith with `food-agent-domain`, `food-agent-application`, `food-agent-api`, `food-agent-infrastructure`, and `food-agent-boot` modules.
- Keep controllers and HTTP DTOs together in the API module; keep use cases and ports in the application module; keep external technology implementations in infrastructure; assemble them only in boot.
- Use Spring AI 2.0.1 only as the Ollama/model client behind the project-owned `LocalModelGateway` interface.
- Use Ollama as the first local model runtime.
- Default to configurable `qwen3:8b`, an 8K context window, temperature `0.1`, one application request at a time, and a 60-second application timeout.
- Use Jakarta Bean Validation plus Spring AI's JSON-schema converter to validate structured responses.
- Use virtual threads to isolate blocking model calls and enforce an application timeout/cancellation boundary.
- Use an in-process deterministic fake gateway for tests. Do not require Ollama during automated tests.
- Use React and TypeScript with Vite when frontend work begins. Do not adopt Next.js without a concrete server-rendering or deployment requirement.
- Do not add a database, vector store, Docker, authentication system, or distributed infrastructure in M1.

## Consequences

- Model/runtime dependencies do not enter domain code.
- Maven enforces the primary dependency direction instead of relying only on package conventions.
- The structure adds some build configuration, but provider-specific modules are deferred until their complexity justifies Option 3-style adapters.
- Ollama remains a separately installed process rather than being embedded in the application.
- The backend can change model providers by adding a gateway implementation.
- The application timeout can return control promptly, although cancellation of underlying inference remains subject to the client/runtime transport.
- Provider tools and the final agent loop are intentionally deferred to later milestones.
