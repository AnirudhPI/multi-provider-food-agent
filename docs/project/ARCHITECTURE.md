# Architecture

## 1. Architectural intent

The system is a modular monolith with explicit ports around volatile dependencies: the local LLM and food providers. This keeps the first release easy to run and debug while preserving clean seams for Swiggy, Zomato, and future model/runtime changes.

The LLM interprets language and proposes structured actions. It does not calculate totals, normalize provider payloads, decide whether confirmation exists, or directly own credentials.

The backend uses Maven modules to enforce the main dependency boundaries:

```text
food-agent-boot
├── food-agent-api
└── food-agent-infrastructure
            |
            v
 food-agent-application
            |
            v
   food-agent-domain
```

Provider-specific modules may be extracted later if the two adapters develop independent dependency or release needs. They remain within `food-agent-infrastructure` until that complexity exists.

## 2. System context

```text
User
  |
  v
React Web UI
  | HTTPS / streaming responses
  v
Backend API (modular monolith)
  |
  +--> Conversation / Agent Orchestrator <--> Local LLM runtime
  |
  +--> Domain services (search, ranking, comparison, cart, confirmation)
  |
  +--> Provider registry and common provider port
          |                              |
          v                              v
     Swiggy adapter                 Zomato adapter
          | MCP                          | MCP
          v                              v
     Swiggy services                Zomato services
```

## 3. Layers and responsibilities

### 3.1 React web UI

- Collects conversational input and renders streamed progress/results.
- Provides structured result cards, comparison views, cart editing, and a dedicated confirmation screen.
- Displays provider identity, unavailable data, stale-data warnings, and errors.
- Holds no provider credentials and does not call provider MCP servers directly.
- Treats backend responses as data; business rules and confirmation enforcement remain server-side.

### 3.2 Backend API

- Defines the browser-facing API and session boundary.
- Authenticates/identifies the local session as required by the deployment mode.
- Validates inputs and maps domain errors to stable client errors.
- Streams assistant text and structured events without exposing internal chain-of-thought.

### 3.3 Agent orchestrator

- Converts conversation plus compact session state into a model request.
- Exposes an allow-listed catalog of application tools with validated schemas.
- Validates model output, executes tool calls, feeds bounded observations back to the model, and terminates within configured step/time limits.
- Separates read-only tools from consequential tools.
- Delegates facts, arithmetic, filtering, normalization, and policy enforcement to deterministic services.
- Records an auditable action trace without storing hidden reasoning or secrets.

### 3.4 Local model gateway

- Provides one internal interface independent of the chosen runtime/model.
- Handles structured-output configuration, timeouts, cancellation, health checks, and model metadata.
- Allows deterministic fake/model fixtures in tests.
- Receives only the minimum context and provider data needed for the current turn.

### 3.5 Domain services

- Own canonical models and rules for search criteria, money, restaurant/menu data, offers, delivery estimates, comparison, cart state, and confirmation.
- Perform deterministic ranking and calculations.
- Track provenance and freshness for values that may change.
- Contain no MCP- or UI-specific types.

### 3.6 Provider port and adapters

The common provider port advertises capabilities rather than assuming every provider supports identical operations. Candidate operations are:

- health/authentication status;
- location resolution;
- restaurant/dish search;
- restaurant/menu detail;
- cart validation or creation;
- quote/totals refresh;
- checkout preparation;
- order placement or provider-hosted handoff;
- order status.

Each adapter:

- maps canonical requests to current MCP tool calls;
- validates and maps MCP results to canonical responses;
- retains provider-native IDs needed for later calls;
- classifies provider/auth/rate-limit/validation/timeout errors;
- contains provider-specific retry and capability behavior;
- does not leak raw provider schema into the domain or UI.

The port must be finalized only after inspecting the actual Swiggy and Zomato MCP manifests. Unsupported operations remain explicit capabilities, not adapter stubs that pretend to work.

### 3.7 Session state

Initial state is session-scoped and contains only what is needed for a coherent journey:

- location reference and user constraints;
- recent normalized result references and freshness;
- selected provider, restaurant, items, and customizations;
- current cart/quote;
- pending consequential action and confirmation nonce/version;
- correlation metadata.

Start with the simplest storage that satisfies the chosen deployment mode. Introduce durable persistence only after its retention, privacy, and multi-user requirements are approved.

## 4. Canonical data model principles

- Use stable internal IDs plus provider and provider-native IDs.
- Represent money precisely with amount-in-minor-units and ISO currency.
- Separate item base price, fees, taxes, discounts, delivery charges, and final total when available.
- Preserve `unknown` separately from zero or false.
- Attach `observedAt`, optional expiry, and source/provider to volatile facts.
- Compare only semantically equivalent values; label non-comparable values.
- Version internal contracts and normalization fixtures.

Indicative domain types include `SearchCriteria`, `RestaurantSummary`, `MenuItem`, `Offer`, `DeliveryQuote`, `ProviderQuote`, `Comparison`, `Cart`, `CheckoutPreview`, and `OrderResult`. Exact fields are a milestone deliverable, not fixed by this document.

## 5. Core request flows

### 5.1 Search and compare

```text
UI request
  -> API validation/session load
  -> orchestrator interprets intent
  -> domain selects capable providers
  -> adapters call MCP tools concurrently with bounds
  -> adapters normalize results
  -> domain filters/ranks/compares
  -> orchestrator explains grounded results
  -> UI renders structured data and explanation
```

Partial success is valid: if one provider fails, successful results may be returned with a clear provider error. Results must not be silently treated as exhaustive.

### 5.2 Checkout

```text
Selected items
  -> provider validates cart and returns fresh quote
  -> backend creates versioned checkout preview
  -> UI shows all known charges and warnings
  -> user explicitly confirms that preview
  -> backend verifies confirmation matches current preview
  -> adapter invokes order/handoff operation once
  -> authoritative result is displayed
```

Any cart mutation or expired/changed quote invalidates the prior confirmation. Ambiguous timeouts after a consequential call require status reconciliation before retry.

## 6. Security and privacy boundaries

- Store secrets in environment/configuration facilities outside source control.
- Keep OAuth tokens, cookies, MCP credentials, addresses, and payment data out of prompts and logs unless an operation strictly requires a scoped reference.
- Validate all model-generated tool arguments against schemas and application policy.
- Allow-list tools; the model cannot choose arbitrary network targets or execute code.
- Require server-side confirmation checks for consequential actions; UI confirmation alone is insufficient.
- Use least-privilege provider scopes and redact structured logs.
- Treat MCP results as untrusted external input and validate them.
- Add rate, step, response-size, and timeout limits at model and provider boundaries.

## 7. Reliability and observability

- Correlate a user turn across API, model, domain, and provider calls.
- Use bounded concurrency for multi-provider search.
- Retry only safe/idempotent reads by default, with jitter and provider-aware limits.
- Add circuit-breaking/health behavior only if real failure patterns justify it.
- Emit metrics for latency, success, tool-selection validity, provider errors, normalization failures, and confirmation-policy violations.
- Never log raw secrets or full personal payloads.

## 8. Testing strategy

- Unit tests for normalization, money, ranking, cart transitions, and confirmation rules.
- Contract tests per provider adapter using sanitized fixtures captured from documented MCP responses.
- Orchestrator tests with a fake local model and scripted tool calls.
- Evaluation cases for intent detection, tool choice, grounding, context updates, and refusal to bypass confirmation.
- UI component and end-to-end tests for the search-to-confirmation journey.
- A small opt-in live integration suite for configured provider accounts; it must never place a real order by default.

## 9. Technology decision gates

Record lightweight decisions before introducing or changing implementation technologies. The M1 baseline is documented in `docs/decisions/0001-m1-technology-baseline.md`: Java 21, Spring Boot, Maven, Spring AI behind a project-owned gateway, and Ollama with configurable `qwen3:8b`. Remaining decision areas are:

1. MCP client library/transport and provider auth flows;
2. frontend component/test stack around React and TypeScript;
3. session persistence;
4. packaging and deployment approach.

Choose the smallest stack that supports streaming, schema validation, MCP connectivity, testability, and clear local setup.

## 10. Dependency rule

Dependencies point inward:

```text
UI / MCP / model infrastructure -> application orchestration -> domain
```

Domain code must not import React, MCP SDK, provider payload, or model-runtime types. This rule is the main defense against provider and AI-framework churn.
