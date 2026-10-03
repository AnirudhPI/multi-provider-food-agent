# Development Plan

## Working method

- Work milestone by milestone; do not begin the next milestone until the current one is demonstrably working and tested.
- Keep changes small enough to review and explain.
- Update the planning documents in `docs/project/` and relevant decision records when behavior or architecture changes.
- End each milestone with automated validation, a short manual demo, documented limitations, and a review checkpoint.
- No implementation begins until the planning baseline is reviewed and approved.

## M0 — Specification and architecture baseline (complete)

**Outcome:** the repository contains an agreed source of truth and a staging branch.

- Create `SPEC.md`, `ARCHITECTURE.md`, `PLAN.md`, and `AGENTS.md` under `docs/project/`.
- Capture goals, non-goals, safety boundaries, milestones, and open decisions.
- Create and work on the `development` branch for the planning baseline.
- Use a dedicated feature branch for every subsequent milestone or feature and open a pull request targeting `development` for user review.
- Review and resolve the open decisions needed for M1.

**Exit criteria:** documents are approved; no product implementation has been added; technology decisions required for M1 are recorded.

## M1 — Local model foundation (current)

**Outcome:** the backend can send a constrained request to a selected local model and validate structured output.

- Decide backend framework, local runtime/model, and package layout.
- Scaffold only the backend modules and tests needed for this milestone.
- Implement model gateway, health check, timeouts, cancellation, structured output validation, and a deterministic fake.
- Document hardware/setup expectations and measure a small latency baseline.

**Exit criteria:** one command runs tests; one documented command starts the backend; a test/demo proves valid structured output and safe handling of malformed output or unavailable model.

## M2 — Swiggy MCP vertical slice

**Outcome:** a user request can produce grounded Swiggy restaurant or dish search results through the backend.

- Inspect current official Swiggy MCP manifest, terms, transport, authentication, and available tools.
- Record a capability matrix and auth/security decision.
- Implement the MCP client boundary and Swiggy adapter for the minimum read-only search/detail slice.
- Define the first canonical search/result models and sanitized contract fixtures.
- Add timeout, provider-error, and unauthenticated behavior.

**Exit criteria:** mocked contract tests pass; an opt-in live smoke test returns normalized Swiggy results; no secrets appear in source, prompts, test fixtures, or logs.

## M3 — Agent/tool-calling loop

**Outcome:** the local model can interpret natural language and invoke allow-listed read-only application tools safely.

- Define typed application tools over domain services, not raw MCP calls.
- Implement bounded orchestration steps, schema validation, observation limits, and cancellation.
- Add trace events suitable for UI streaming and debugging.
- Add evaluations for search intent, clarifying questions, invalid tool arguments, and tool failure.

**Exit criteria:** representative natural-language requests consistently select valid tools and return grounded responses; arbitrary tools/arguments are rejected.

## M4 — React user interface

**Outcome:** a user can complete the Swiggy discovery flow in the standalone web UI.

- Select the React build/test stack and record the decision.
- Implement chat input, streamed progress, structured result cards, refinements, provider/error states, and accessibility basics.
- Keep browser-facing contracts typed and versioned.
- Add component and end-to-end coverage for the vertical slice.

**Exit criteria:** the search/refinement flow works from UI to Swiggy MCP; loading, empty, timeout, and auth-error states are usable.

## M5 — Zomato MCP integration

**Outcome:** the same read-only discovery use cases work with Zomato when its official MCP capabilities allow them.

- Inspect current official Zomato MCP manifest, terms, transport, authentication, and tools.
- Extend the capability matrix.
- Implement and contract-test the Zomato adapter.
- Surface unsupported capabilities explicitly.

**Exit criteria:** mocked contract tests pass; an opt-in live smoke test returns normalized Zomato results or a documented external limitation blocks the capability without breaking Swiggy.

## M6 — Provider abstraction hardening

**Outcome:** provider-specific behavior is contained behind a capability-aware common port.

- Reconcile both real provider schemas and refine canonical models.
- Remove provider schema leakage from application/domain/UI layers.
- Add provider registry, health/capability reporting, provenance, freshness, and error taxonomy.
- Test each application use case against adapter fakes for both providers.

**Exit criteria:** core use cases run unchanged against either provider; unsupported operations are explicit; domain code has no MCP/provider imports.

## M7 — Cross-provider comparison

**Outcome:** users receive honest, deterministic comparisons across available providers.

- Define comparable price/fee/ETA/rating semantics and missing-data rules.
- Implement deterministic filtering, ranking, tie-breaking, and explanation inputs.
- Query providers concurrently with latency and result bounds.
- Add fixture-based arithmetic, fairness, partial-failure, and stale-data tests.

**Exit criteria:** the same fixtures always produce the same comparison; every displayed claim is traceable to normalized provider data or a disclosed rule.

## M8 — Conversation context and state

**Outcome:** multi-turn refinement and selection work reliably within a session.

- Define the session state machine and lifecycle.
- Store compact structured state rather than replaying unbounded transcripts.
- Implement correction, clearing, expiry, and stale-result behavior.
- Decide whether persistence beyond process/session lifetime is needed.

**Exit criteria:** multi-turn evaluation cases pass without leaking state between sessions; users can inspect or reset material context.

## M9 — Cart and human-confirmed checkout

**Outcome:** a user can prepare a provider cart and reach a safe, explicit confirmation boundary; real order execution is included only if approved and supported.

- Inspect provider cart/checkout/order capabilities and idempotency behavior.
- Implement cart state, validation, quote refresh, versioned checkout preview, and confirmation state machine.
- Separate read-only preparation tools from consequential tools.
- Implement reconciliation for ambiguous failures and ensure tests cannot place live orders.
- If real ordering is not approved or safely supported, hand off to provider-hosted checkout and document the boundary.

**Exit criteria:** no order/handoff occurs without a confirmation matching the current quote; mutations invalidate confirmation; duplicate execution is prevented or reconciled.

## M10 — Evaluation, hardening, and polish

**Outcome:** a documented, reproducible, portfolio-ready release.

- Expand end-to-end evaluations and establish pass thresholds.
- Perform threat modeling, dependency/security checks, accessibility review, and failure testing.
- Improve logs/metrics, setup scripts, README, architecture decisions, limitations, and demo flow.
- Test a clean setup on the documented environment.

**Exit criteria:** all automated checks pass; the demo journey is reproducible; known limitations and security boundaries are documented; release approval is explicit.

## Suggested four-week pacing

- **Week 1 (~8 hours of review/active effort):** M0–M2.
- **Week 2 (~8 hours):** M3–M4.
- **Week 3 (~10 hours):** M5–M7.
- **Week 4 (~10 hours):** M8–M10.

This is a planning target, not a deadline. External MCP/auth behavior is the largest schedule risk.

## Review gates

Human review is required before:

- starting implementation after M0;
- adopting the backend/model/runtime stack;
- introducing durable storage or new infrastructure;
- sending personal data to any model/provider beyond the provider operation's minimum need;
- enabling live checkout or order placement;
- merging `development` into `main`.

## Branch and review workflow

1. Keep `main` as the stable branch and `development` as the integration/staging branch.
2. Start each feature, fix, documentation change, or milestone from the latest `development` branch.
3. Use a short-lived branch named `feature/<description>`, `fix/<description>`, or `docs/<description>` as appropriate.
4. Keep commits focused and include the relevant tests and documentation.
5. Push the branch and open a pull request whose base branch is `development`.
6. Do not merge the pull request; the user reviews and merges it.
7. Do not push directly to `development` or `main` after this planning-baseline commit unless the user explicitly requests it.

## Initial risk register

| Risk | Impact | Mitigation |
|---|---|---|
| Provider MCP/auth capability differs from assumptions | Blocks or narrows journeys | Run capability spikes before designing full ports; degrade explicitly |
| Local model produces invalid or unreliable tool calls | Incorrect behavior | Constrained schemas, validation, bounded loop, deterministic services, evaluations |
| Provider schemas are not semantically comparable | Misleading recommendations | Preserve provenance/missingness; compare only equivalent fields; document rules |
| Price/availability changes during a session | Unsafe or frustrating checkout | Fresh quote before confirmation; expiry and invalidation |
| Duplicate order after timeout/retry | Financial/user harm | Idempotency where supported; reconciliation before retry; explicit uncertain state |
| Sensitive data reaches prompts/logs/source | Privacy/security harm | Minimize, redact, reference by opaque IDs, secret scanning, least privilege |
| Scope expands into unnecessary infrastructure | Delays and obscures learning goals | Modular monolith, milestone exits, documented decision gates |
