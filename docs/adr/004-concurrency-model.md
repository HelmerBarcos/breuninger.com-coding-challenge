# 004. Concurrency: coroutine fan-out with per-module degradation

Date: 2026-07-03
Status: Accepted

## Context

Each feed module conceptually calls a different backend (catalog, campaigns,
order history). Resolving them sequentially would make feed latency the *sum*
of module latencies; a homefeed is the app's first screen, so latency matters.
Module data sources are simulated with configurable fake latency to make the
concurrency behavior observable and testable.

## Options considered

| Criterion | A) Sequential resolution | B) `CompletableFuture` / virtual threads | C) WebFlux + reactive stack | D) Kotlin coroutines on Spring MVC (chosen) |
|---|---|---|---|---|
| Feed latency | Sum of providers | Max of providers | Max of providers | Max of providers |
| Fit with Kotlin | — | Java-idiomatic | Reactor types leak everywhere | Idiomatic (`suspend`, structured concurrency) |
| Fit with JPA (blocking) | Trivial | Good | Poor — needs R2DBC migration | Good (`Dispatchers.IO`) |
| Complexity budget | None | Medium | High for this scope | Medium |
| Cancellation/error semantics | Trivial | Manual | Built-in | Structured concurrency built-in |

**C** is the "fully async" answer but drags R2DBC and Reactor into the codebase
for no measurable user benefit here. **B** works but forfeits Kotlin's main
concurrency strength. **D** keeps blocking JPA (wrapped in `Dispatchers.IO`),
runs on plain Spring MVC (suspend controller support since Spring Boot 3.2) and
gives structured concurrency for free.

## Decision

`BngrHomefeedService.assembleFeed` runs `coroutineScope { providers.map { async
{ it.provide(ctx) } }.awaitAll() }`. Each provider call is wrapped in
`runCatching`: a failing provider is logged and its module omitted — **the feed
degrades, it never 500s** because one banner backend is down. Simulated latency
is a typed configuration property (`homefeed.simulation.latency`), set to zero
in tests. The response envelope exposes `meta.assemblyTimeMs`, making the
fan-out empirically visible (total ≈ max, not sum).

## Consequences

- Fan-out and degradation are observable in the API response itself.
- Unit tests use `kotlinx-coroutines-test` (`runTest`) with virtual time — no
  real sleeping in the test suite.
- If persistence ever becomes reactive, providers keep their signatures
  (`suspend`) and only the repository adapters change.

## Enforcement

Unit tests assert: (1) assembly time ≈ max provider latency under virtual
time; (2) a throwing provider results in an omitted module and an otherwise
complete feed, not an error response.
