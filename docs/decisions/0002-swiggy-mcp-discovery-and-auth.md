# ADR 0002: Swiggy MCP discovery and authentication

- **Status:** accepted for M2
- **Date:** 2026-11-02
- **Scope:** Swiggy read-only restaurant discovery vertical slice

## Context

M2 needs one grounded Swiggy discovery flow without leaking provider or MCP types into the domain. The decision is based on Swiggy's official [Builders documentation](https://mcp.swiggy.com/builders/docs/), [Food tool reference](https://mcp.swiggy.com/builders/docs/reference/food/), [authentication guide](https://mcp.swiggy.com/builders/docs/start/authenticate/), and [official manifest repository](https://github.com/Swiggy/swiggy-mcp-server-manifest), inspected on 2026-11-02.

The live endpoint was also probed with a non-consequential MCP `initialize` request and no credential. It returned HTTP 401 with OAuth protected-resource metadata. The assembled backend was then verified to map that response to HTTP 401 `PROVIDER_NOT_AUTHENTICATED`. No account data, personal data, or write tool was used.

On 2026-11-02, the opt-in authenticated smoke test completed successfully against `search_restaurants` using runtime-supplied credentials and coordinates. The live SDK response exposed the tool-specific result directly through MCP `structuredContent`; the adapter normalized it without retaining or documenting the credential, coordinates, or returned account/provider data.

## Capability matrix

| Capability | Official Food tool | M2 status | Notes |
|---|---|---|---|
| Saved addresses | `get_addresses` | Not exposed | Authenticated and contains personal data; coordinates are sufficient for this slice |
| Restaurant discovery | `search_restaurants` | Implemented | Query plus latitude/longitude; normalizes restaurant summaries and pagination |
| Dish discovery | `search_menu` | Deferred | Numeric item-price unit is not explicit enough to normalize money safely without a verified live contract |
| Restaurant menu | `get_restaurant_menu` | Deferred | Same price-unit concern; restaurant IDs are preserved for this follow-up |
| Address recommendation | `recommend_address` | Deferred | Not required when the API receives validated coordinates |
| Cart, coupon, payment, ordering, tracking | Multiple write/read tools | Prohibited in M2 | Consequential flows belong to M9 and require confirmation safeguards |

Swiggy currently documents a uniform tool envelope: `{success, data, message}` on success and `{success:false, error:{message}}` on domain failure. The live MCP SDK response exposed the successful tool-specific data directly through `structuredContent`, without the outer envelope, so the decoder accepts both shapes while still requiring a valid restaurant array. Transport/auth failures can also use HTTP status and JSON-RPC codes. The adapter treats all payloads as untrusted, bounds payload/result size, requires restaurant names, and preserves an absent optional native identifier as unknown. Swiggy documents `error.message` as the primary domain-failure contract. If a failure envelope omits that field, the adapter may use a separate human-readable MCP text block as a fallback. In either case it applies whitespace sanitization and a 300-code-point limit; it never exposes the complete provider payload, report metadata, or credentials.

## Decision

1. Use the official MCP Java SDK through Spring AI's `spring-ai-starter-mcp-client` with the synchronous JDK `HttpClient` Streamable HTTP transport. The backend is Spring MVC, so M2 does not add a reactive client stack solely for this one integration.
2. Configure one Food connection at `https://mcp.swiggy.com/food`, disabled by default. Initialization is lazy so the backend can start without provider authentication and classify a first-call 401 cleanly.
3. Keep MCP SDK types behind the infrastructure-local `SwiggyMcpToolClient`. The application sees only `RestaurantDiscoveryGateway`; domain/API layers see canonical records.
4. Hard-code the only M2 tool name, `search_restaurants`, inside the adapter. The model and API caller cannot choose an arbitrary MCP tool.
5. Use Swiggy OAuth 2.1 with PKCE as the authority. For the M2 local smoke test, accept a temporary access token only through `SWIGGY_MCP_ACCESS_TOKEN`; never store it in source, fixtures, prompts, or logs.
6. Do not implement token persistence or automatic refresh in M2. Browser-based delegated login, per-user token storage, refresh, and per-user MCP session lifecycle must be designed with the UI/session milestone before multi-user use.

## Authentication constraints

- Swiggy requires OAuth for every external caller, including read-only discovery.
- Swiggy v1 scopes are server-level. `mcp:tools` is not split into read/write scopes, so the credential can authorize more than M2 uses. The hard-coded read-only adapter is the local least-authority control.
- Access tokens currently live for five days and can be revoked earlier. Missing or expired authentication maps to `PROVIDER_NOT_AUTHENTICATED`; tokens are never included in that response.
- Spring AI's auto-configured MCP client is one application-scoped protocol session. This is acceptable for the single-user local M2 demo only and must not be treated as a user/tenant boundary.

## Consequences

- The backend has a testable, provider-independent restaurant search use case and a small SDK adapter.
- Mocked contract tests are deterministic and use only sanitized data.
- A live smoke test requires the developer to complete Swiggy's OAuth flow and supply the temporary token and test coordinates at runtime.
- Dish/menu price normalization remains blocked until the provider contract or a sanitized live response establishes exact money semantics; the system does not guess.
- The opt-in live smoke test succeeded with an authenticated local account on 2026-11-02, satisfying the final M2 external-integration exit criterion.
