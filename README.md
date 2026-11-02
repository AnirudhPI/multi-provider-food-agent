# Multi-Provider Food Agent

A standalone conversational application that will search and compare food options across Swiggy and Zomato through supported MCP interfaces. The project uses a React and TypeScript frontend, a Java 21/Spring Boot backend, and a local language model served by Ollama.

The project is developed incrementally. See [`docs/project/`](docs/project/) for its specification, architecture, development plan, and repository instructions.

## Current milestone

M2 adds the first read-only Swiggy MCP vertical slice while retaining the M1 local-model foundation:

- provider-neutral restaurant search models and application service;
- a Swiggy `search_restaurants` adapter over MCP Streamable HTTP;
- sanitized contract fixtures and normalized restaurant results;
- explicit unavailable, unauthenticated, timeout, and invalid-response errors;
- an opt-in, read-only live smoke test.

The backend is a Maven modular monolith:

```text
backend/
├── food-agent-domain/          # Framework-independent domain rules
├── food-agent-application/     # Use cases and outbound ports
├── food-agent-api/             # REST controllers and HTTP DTOs
├── food-agent-infrastructure/  # Ollama and provider/MCP adapters
└── food-agent-boot/            # Executable application and wiring
```

Dependencies point inward: `boot` assembles `api` and `infrastructure`; both depend on `application`, which depends on `domain`.

The frontend, agent loop, Zomato adapter, menu/cart, and ordering flows have not been implemented yet.

## Prerequisites on macOS

Install Java 21 through Homebrew:

```bash
brew install openjdk@21
```

Make it active in the current shell:

```bash
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
```

Verify that both Java and Maven use version 21:

```bash
java -version
cd backend
./mvnw -version
```

Install Ollama using its supported installation method, start it, and download the configured model:

```bash
ollama pull qwen3:8b
```

## Build and test

```bash
cd backend
./mvnw test
```

Tests use a deterministic fake gateway and do not require Ollama.

## Run

```bash
cd backend
./mvnw clean package
java -jar food-agent-boot/target/food-agent-boot-0.0.1-SNAPSHOT.jar
```

The executable application and runtime configuration belong to `food-agent-boot`; the other modules produce ordinary library JARs.

Configuration can be changed with environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama API URL |
| `OLLAMA_MODEL` | `qwen3:8b` | Local model tag |
| `OLLAMA_TEMPERATURE` | `0.1` | Sampling temperature |
| `OLLAMA_CONTEXT_WINDOW` | `8192` | Context window |
| `MODEL_TIMEOUT` | `60s` | Maximum application wait for a model call |
| `MODEL_PROVIDER` | `ollama` | Use `fake` for a deterministic local demo |
| `SWIGGY_MCP_ENABLED` | `false` | Enables the Swiggy Food MCP connection |
| `SWIGGY_MCP_ACCESS_TOKEN` | empty | Temporary OAuth access token; never commit or log it |
| `SWIGGY_MCP_BASE_URL` | `https://mcp.swiggy.com` | Swiggy MCP origin |
| `SWIGGY_MCP_TIMEOUT` | `20s` | Maximum MCP request duration |

### Model API

```bash
curl http://localhost:8080/api/v1/model/health

curl http://localhost:8080/actuator/health

curl -X POST http://localhost:8080/api/v1/model/generate \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Find me a vegetarian dosa"}'
```

The generation endpoint is an M1 integration proof, not the final conversation or agent API.

### Swiggy restaurant API

With Swiggy disabled, the endpoint returns `PROVIDER_UNAVAILABLE`. When enabled, every call requires a Swiggy OAuth token:

```bash
curl 'http://localhost:8080/api/v1/providers/swiggy/restaurants?query=dosa&latitude=12.9716&longitude=77.5946'
```

Swiggy uses OAuth 2.1 with PKCE rather than a static API key. Complete the official [Authenticate flow](https://mcp.swiggy.com/builders/docs/start/authenticate/) yourself, then provide the temporary access token only in the process environment. To avoid putting the secret in shell history:

```bash
read -s SWIGGY_MCP_ACCESS_TOKEN
export SWIGGY_MCP_ACCESS_TOKEN
export SWIGGY_MCP_ENABLED=true
```

The M2 adapter can call only `search_restaurants`; it cannot invoke cart or order tools. Token persistence, refresh, and browser login are deliberately deferred until the UI/session design can provide a correct per-user lifecycle.

### Opt-in Swiggy live smoke test

The ordinary test suite never calls Swiggy. After authenticating, provide test coordinates outside source control and opt in explicitly:

```bash
export SWIGGY_MCP_LIVE_TEST=true
export SWIGGY_MCP_TEST_LATITUDE='<latitude>'
export SWIGGY_MCP_TEST_LONGITUDE='<longitude>'
cd backend
./mvnw -Dtest=SwiggyLiveSmokeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

The smoke test performs restaurant discovery only. It does not modify a cart or place an order.

## Local model baseline

The M1 live smoke test was run on an Apple M3 Pro with 18 GB unified memory using Ollama `0.40.3` and `qwen3:8b` (`Q4_K_M`) with an 8192-token context window. Ollama reported 100% GPU execution.

| Scenario | Result |
|---|---|
| Model unloaded, first structured request | 7.37 seconds |
| Model resident, repeated structured request | 3.78 seconds |
| Configured model health | HTTP 200, `READY` |
| Missing model health | HTTP 503, `MODEL_NOT_INSTALLED` |
| Unreachable Ollama health | HTTP 503, `OLLAMA_UNREACHABLE` |

These are single local smoke-test measurements rather than a performance benchmark. Structured output validation succeeded for the tested food and unsupported requests. Classification of an ambiguous request containing “near me” varied between `FOOD_REQUEST` and `CLARIFICATION`, so intent quality still needs the evaluation suite planned for M3.
