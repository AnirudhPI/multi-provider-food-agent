# ADR 0003: Bounded project-owned agent orchestration

- **Status:** Accepted for M3
- **Date:** 2026-11-03

## Context

M1 established a replaceable local-model gateway and M2 established a provider-neutral restaurant discovery service backed by Swiggy MCP. M3 must let the local model select a read-only application operation without giving the model direct access to raw MCP tools, credentials, network targets, or unbounded framework-managed tool execution.

Spring AI can automatically execute registered tool callbacks, but using that mechanism as the orchestration boundary would make it harder for the application to enforce one consistent policy for allowed tool names, arguments, total steps, observation sizes, cancellation, trace events, and future separation of read-only and consequential operations.

Location also has multiple future sources. It can initially arrive as request coordinates, then later come from browser geolocation, a provider-supported saved location, or application-managed session state. The model should not infer or generate coordinates.

## Decision

Use a project-owned bounded orchestration loop in `food-agent-application`.

The model returns one schema-validated action per step:

- `ToolCall` with an exact tool name and typed arguments;
- `FinalAnswer` after a tool observation;
- `Clarification` when required context is missing;
- `Unsupported` for requests outside food discovery.

The only M3 tool is the read-only `search_restaurants` application tool. It calls `RestaurantDiscoveryService`; it does not expose Swiggy MCP or Spring AI types. The application rejects unknown tools, additional arguments, invalid queries, and final answers without observations.

Each turn has configurable limits for total steps, overall duration, observation characters, and restaurant results. Model and tool calls run as cancellable virtual-thread tasks. Provider failures are reduced to safe categories before being returned as observations. Trace events carry a generated turn ID and operational metadata but no chain-of-thought.

Resolved coordinates remain in `AgentTurnContext`. The model receives only a boolean indicating whether location exists, and `search_restaurants` receives only `query` as a model-generated argument.

## Consequences

- Application policy remains independent of Ollama, Spring AI, and provider MCP schemas.
- Deterministic scripted model/provider tests can cover every action and failure path.
- Adding a tool requires explicit registration, schema handling, observation formatting, and tests.
- The synchronous M3 API returns accumulated trace events; M4 may stream the same event model without changing the orchestrator contract.
- Native framework tool-calling conveniences are intentionally not the security boundary.
- Semantic grounding still depends partly on model behavior, so structured provider results remain authoritative and opt-in live evaluations supplement deterministic tests.
