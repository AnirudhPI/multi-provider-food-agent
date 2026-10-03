# Multi-Provider Food Agent

A standalone conversational application that will search and compare food options across Swiggy and Zomato through supported MCP interfaces. The project uses a React and TypeScript frontend, a Java 21/Spring Boot backend, and a local language model served by Ollama.

The project is developed incrementally. See [`docs/project/`](docs/project/) for its specification, architecture, development plan, and repository instructions.

## Current milestone

M1 establishes the local-model boundary:

- Java 21 and Spring Boot with Maven;
- Spring AI isolated behind `LocalModelGateway`;
- Ollama with configurable `qwen3:8b` defaults;
- structured-output decoding and validation;
- model health, timeout, cancellation, and deterministic fake support.

The backend is a Maven modular monolith:

```text
backend/
├── food-agent-domain/          # Framework-independent domain rules
├── food-agent-application/     # Use cases and outbound ports
├── food-agent-api/             # REST controllers and HTTP DTOs
├── food-agent-infrastructure/  # Spring AI and Ollama adapters
└── food-agent-boot/            # Executable application and wiring
```

Dependencies point inward: `boot` assembles `api` and `infrastructure`; both depend on `application`, which depends on `domain`.

No provider integration or frontend has been implemented yet.

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

### Model API

```bash
curl http://localhost:8080/api/v1/model/health

curl -X POST http://localhost:8080/api/v1/model/generate \
  -H 'Content-Type: application/json' \
  -d '{"prompt":"Find me a vegetarian dosa"}'
```

The generation endpoint is an M1 integration proof, not the final conversation or agent API.
