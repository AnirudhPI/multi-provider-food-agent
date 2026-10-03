# Product Specification

## 1. Product summary

`multi-provider-food-agent` is a standalone conversational food-discovery and ordering application. A user describes what they want in natural language; a locally run language model interprets the request and orchestrates provider tools exposed through MCP. The application searches Swiggy and Zomato, normalizes their different responses, compares viable choices, and helps the user build a cart. Any action that can place an order or create a financial commitment requires explicit user confirmation.

The application has its own web UI and backend. It must not depend on the ChatGPT UI or a ChatGPT subscription at runtime.

## 2. Goals

- Provide a natural-language interface for restaurant and dish discovery.
- Integrate Swiggy first, then Zomato, through provider-supported MCP interfaces.
- Hide provider-specific schemas behind a common provider contract.
- Compare providers using deterministic normalized data rather than free-form model guesses.
- Preserve useful conversation and selection context during a session.
- Support a safe cart and checkout flow with clear human confirmation boundaries.
- Run the reasoning model locally where practical.
- Produce an understandable, testable portfolio project whose architectural choices can be explained and defended.

## 3. Non-goals for the first release

- Building a marketplace, payment processor, delivery network, or restaurant-management system.
- Scraping provider websites or reverse-engineering private APIs.
- Autonomous purchase, payment, or checkout without an explicit final confirmation.
- Long-term personalization, social features, recommendations trained on many users, or a mobile app.
- Supporting providers other than Swiggy and Zomato.
- Introducing distributed infrastructure such as Kafka, Kubernetes, multiple databases, or microservices without a demonstrated need.
- Replacing provider authentication, cart, payment, or order systems.

## 4. Primary user journeys

### 4.1 Discover food

1. The user states a need such as cuisine, dish, budget, dietary preference, delivery speed, rating, or location.
2. The assistant identifies missing required constraints and asks a focused follow-up when necessary.
3. The system searches one or both available providers.
4. Results are normalized, filtered, ranked, and presented with their provider and relevant trade-offs.
5. The user can refine the request without restating prior session context.

### 4.2 Compare providers

1. The user asks for the best option or explicitly requests a comparison.
2. The system queries providers using equivalent constraints when provider capabilities allow it.
3. It compares normalized price, fees, delivery estimate, rating, availability, and other supported fields.
4. Missing or stale values are shown as unknown rather than invented.
5. The response explains the basis of the recommendation and retains links or provider identifiers needed for follow-up actions.

### 4.3 Build a cart and check out

1. The user chooses a provider, restaurant, items, quantities, and supported customizations.
2. The provider adapter validates availability and obtains authoritative totals.
3. The system shows a final summary including provider, items, address reference, charges, total, and any material warnings.
4. The user explicitly confirms the final action.
5. Only then may the provider checkout/order tool be invoked.
6. The result is reported using the provider's authoritative order status and identifier.

## 5. Functional requirements

### FR-1: Conversational interaction

- Accept natural-language requests and follow-ups.
- Distinguish informational requests from state-changing actions.
- Use structured tool calls rather than parsing provider actions from prose.
- Ask for clarification only when a missing value prevents a safe or meaningful action.

### FR-2: Provider capability management

- Detect which provider adapters are configured, authenticated, and healthy.
- Route requests only to providers that support the required capability.
- Degrade gracefully to a single provider and clearly disclose unavailable providers.
- Treat the actual MCP tool manifests as the authority on available operations.

### FR-3: Search and detail retrieval

- Search restaurants and/or dishes using location and user constraints.
- Retrieve sufficient restaurant/menu detail for selection.
- Preserve provider-native identifiers internally for subsequent operations.
- Paginate or limit results to keep latency and model context controlled.

### FR-4: Normalization and comparison

- Convert provider responses into versioned internal domain models before comparison or model presentation.
- Preserve raw provider payloads only at the adapter boundary for diagnostics; do not expose secrets or unnecessary personal data.
- Keep money as currency plus integer minor units (or an equivalently precise type), never binary floating point.
- Track field provenance, freshness, and absence where those affect comparison.
- Apply deterministic filtering, arithmetic, and ranking outside the LLM.

### FR-5: Session context

- Maintain the active location, constraints, recent results, selected provider/restaurant/items, and pending confirmation state.
- Scope state to a user session.
- Allow the user to correct or clear context.
- Do not imply durable memory until durable persistence is explicitly designed and approved.

### FR-6: Cart and checkout safety

- Validate cart operations through the selected provider.
- Re-fetch or validate price and availability before confirmation.
- Require an explicit, unambiguous confirmation immediately before any order-placement or equivalent irreversible action.
- Never expose credentials, tokens, addresses, or payment details to the model unless strictly required and approved by the architecture.
- Make retries idempotent where supported; never silently place a second order after an ambiguous failure.

### FR-7: User interface

- Provide a React-based web interface for chat, results, comparison, cart, confirmation, progress, and errors.
- Make provider identity and data freshness visible.
- Keep consequential actions visually distinct from search/refinement actions.
- Remain usable when only one provider is configured.

### FR-8: Observability and evaluation

- Produce structured logs with correlation IDs and redaction.
- Record tool name, provider, duration, outcome, and error category without secrets.
- Provide repeatable evaluations for intent/tool selection, normalization, comparison math, context handling, confirmation safety, and failure behavior.

## 6. Quality attributes

- **Safety:** no autonomous checkout; least-privilege access; secrets remain server-side.
- **Correctness:** provider facts and calculations come from tools and deterministic code, not model invention.
- **Privacy:** minimize stored and model-visible personal data; redact logs.
- **Reliability:** timeouts, bounded retries, cancellation, and partial-provider failure handling.
- **Portability:** provider and model implementations can be replaced behind stable ports.
- **Testability:** adapters and model calls are mockable; core domain logic is deterministic.
- **Simplicity:** begin as a modular monolith and add infrastructure only in response to measured needs.

## 7. Constraints and assumptions

- Swiggy and Zomato capabilities, authentication, geographic availability, and terms may differ and may change. Implementation begins with a capability spike against current official MCP interfaces.
- The local model must support reliable structured output/tool selection, or the orchestrator must enforce it through validation and constrained schemas.
- Provider data is time-sensitive. The UI must not present cached price, availability, fee, or ETA as current after its validity window.
- Provider credentials and MCP sessions are backend concerns and must not be shipped to the browser.
- Technology choices not settled in the source discussion—backend language/framework, local model/runtime, database, and deployment target—remain explicit decision gates in `docs/project/PLAN.md`.

## 8. Success criteria for the first polished release

- A user can search Swiggy through natural language and receive grounded results.
- A user can repeat the same journey with Zomato when its MCP capabilities permit it.
- Comparable results from both providers appear in a shared schema with correct arithmetic and honest handling of missing fields.
- Follow-up requests correctly use session context and allow corrections.
- A user can prepare a cart and reach a confirmation boundary; no order is placed without explicit confirmation.
- Provider/model outages produce actionable errors rather than fabricated results.
- Automated tests and evaluation fixtures cover the critical paths and safety boundary.
- Setup, architecture, limitations, and demo instructions are documented.

## 9. Open decisions requiring review

- Backend language and framework are resolved for M1: Java 21, Spring Boot, and Maven.
- Local LLM baseline is resolved for M1: Ollama with configurable `qwen3:8b`, initially targeting an Apple M3 Pro with 18 GB unified memory.
- Exact Swiggy and Zomato MCP tools available to the project and their authentication flows.
- Whether the first release executes real order placement or stops at a provider-hosted checkout handoff.
- Session storage approach and whether any durable history is in scope.
- Initial ranking weights and how the UI lets users override them.
- Deployment scope: local-only demonstration versus a remotely hosted backend that can still reach the chosen local model.
