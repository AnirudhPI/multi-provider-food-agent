# Repository Instructions for Coding Agents

## Current phase

This repository is in **M0: specification and architecture review**.

- Do not add product implementation, scaffolding, dependencies, generated projects, containers, CI workflows, or deployment configuration until the user explicitly approves the planning baseline and asks to begin a milestone.
- The four documents in `docs/project/`—`SPEC.md`, `ARCHITECTURE.md`, `PLAN.md`, and this file—are the current source of truth.
- The planning baseline is committed on `development`. For subsequent work, create a short-lived feature branch from `development` and open a pull request back to `development` for user review.
- Do not push feature commits directly to `development` or `main`, and do not merge pull requests, unless the user explicitly instructs you to do so.

## Product invariants

- The product is a standalone React UI plus backend; it must not require the ChatGPT UI at runtime.
- Use a local LLM through a replaceable gateway.
- Integrate Swiggy and Zomato through their supported MCP interfaces; do not scrape or reverse-engineer private APIs.
- Keep provider-specific schemas and MCP details inside adapters.
- Normalize provider data before cross-provider comparison or model presentation.
- Use deterministic code for money, filtering, ranking, validation, and confirmation policy.
- Never place an order or cause a financial commitment without explicit user confirmation against the current checkout preview.
- Prefer a modular monolith. Do not add distributed infrastructure without an approved, documented need.

## How to work

1. Read all four documents in `docs/project/` before making architectural or implementation changes.
2. Confirm the active milestone and its exit criteria in `docs/project/PLAN.md`.
3. Inspect the existing repository and preserve unrelated user changes.
4. If a required technology decision is still open, present the trade-off or record a lightweight decision before scaffolding.
5. Implement the smallest end-to-end change that advances the active milestone.
6. Add or update tests with the change.
7. Run the narrowest relevant checks first, then the milestone's full validation suite.
8. Update documentation when contracts, behavior, commands, or known limitations change.
9. Stop at the milestone review gate; do not silently roll into the next milestone.

## Git workflow

- Begin future work from an up-to-date `development` branch.
- Name branches `feature/<description>`, `fix/<description>`, or `docs/<description>`.
- Keep each branch scoped to one reviewable concern.
- Commit completed, verified work on the feature branch.
- Push the feature branch and open a pull request with `development` as its base.
- Leave the pull request unmerged for the user to review.
- Never force-push, rewrite shared history, or merge into `development` or `main` without explicit instruction.

## Architecture rules

- Keep domain code independent of UI frameworks, MCP SDKs, provider payload types, and model runtime types.
- Depend on interfaces/ports at volatile boundaries; avoid speculative abstraction elsewhere.
- Validate every model-generated tool call and every external MCP response.
- Expose application-level tools to the model, not raw unrestricted MCP or shell access.
- Separate read-only operations from consequential operations in code, schemas, and UI.
- Treat missing provider fields as unknown, not zero or false.
- Represent money precisely; do not use binary floating point.
- Preserve provider/native identifiers and data provenance without leaking them into unrelated layers.
- Bound model steps, provider concurrency, payload sizes, timeouts, and retries.
- Never rely on model prose as the system of record for cart, quote, confirmation, or order state.

## Security and data handling

- Never commit credentials, cookies, OAuth tokens, addresses, payment data, real provider payloads containing personal data, or local environment files.
- Use sanitized fixtures for provider contract tests.
- Keep secrets and provider sessions server-side.
- Redact logs and error messages; prefer opaque references to personal/provider account data.
- Do not send full conversation history or raw provider payloads to the model when a compact structured subset is sufficient.
- Live integration tests must be opt-in and non-consequential by default. Automated tests must never place a real order.
- Treat MCP/tool output as untrusted input.

## Testing expectations

Each implemented behavior must have proportionate automated coverage:

- unit tests for domain rules and normalization;
- provider contract tests using sanitized fixtures;
- orchestrator tests using fake model/provider implementations;
- UI tests for material states and confirmation UX;
- end-to-end tests for milestone-critical journeys;
- evaluation cases for model/tool behavior where ordinary deterministic tests are insufficient.

Tests must cover relevant failure paths, especially invalid model output, auth failure, timeout, partial provider failure, stale quotes, confirmation invalidation, and ambiguous consequential-call results.

Exact commands will be added after the M1 technology decision. Do not invent commands in documentation before the tooling exists.

## Change discipline

- Keep commits and patches focused on one milestone concern.
- Avoid broad reformatting or dependency upgrades unrelated to the task.
- Do not edit generated files by hand once generation exists.
- Do not weaken tests or safety rules merely to make a check pass.
- Record assumptions and unresolved external constraints explicitly.
- If actual provider capabilities contradict these documents, do not fake the promised behavior: update the capability analysis and request review of the affected scope.

## Definition of done for a milestone change

- Acceptance and exit criteria are met.
- Relevant automated checks pass.
- Manual verification is documented when external integrations are involved.
- No secrets or personal data were added.
- Error and partial-failure behavior is covered.
- Documentation and examples match the implementation.
- The user can explain the architectural decision and has a review point before the next milestone.
