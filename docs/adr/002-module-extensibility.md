# 002. Feed module extensibility: sealed type + ModuleProvider discovery

Date: 2026-07-03
Status: Accepted

## Context

The homefeed grows primarily by adding new module types. The goal is
Open/Closed in practice, not in citation: adding a module type must mean adding
classes, never modifying the feed assembly.

## Options considered

| Criterion | A) Enum + central `when` in the service | B) Manual registry (modules registered in a config class) | C) Sealed interface + Spring-discovered providers (chosen) |
|---|---|---|---|
| New module touches existing code | Service + enum + mapping | Registry class | Only the serialization registration points |
| Compile-time safety | Good (exhaustive `when`) | None: forgetting to register fails at runtime | Good: sealed + exhaustive `when` in the DTO mapper |
| Conditional modules (user-dependent) | `if`s accumulate in the service | Per-entry logic | Provider returns `null`; the service stays generic |
| Discoverability | High (one file) | Medium | Medium: convention must be documented |

**A** keeps everything visible but the service grows with every module — the
exact thing the requirement forbids. **B** decouples creation but replaces
compiler guarantees with runtime wiring. **C** uses a sealed domain type plus a
provider port; Spring injects `List<BngrModuleProvider>`, so registration is
automatic at context startup.

## Decision

```kotlin
sealed interface BngrFeedModule

interface BngrModuleProvider {
    val order: Int
    suspend fun provide(ctx: BngrFeedContext): BngrFeedModule?  // null = not applicable
}
```

`BngrHomefeedService` consumes the injected provider list, resolves them
concurrently (ADR-004), filters nulls and sorts by `order`. Conditional logic
(e.g. a module only for authenticated users) lives in the provider via
`BngrFeedContext`, keeping the service free of special cases. A new module =
domain model + provider + DTO; the only existing files touched are the
`BngrModuleTypes` constants, the `@JsonSubTypes` list and the `toDto()` mapper
(ADR-003) — all of which fail compilation or a test when forgotten.

## Consequences

- The `add-feed-module` checklist is fully mechanical (scripted as a Claude
  Code skill).
- Provider ordering via an `order` value needs a documented convention
  (spaced values, e.g. 10/20/30).
- Module-specific wiring mistakes surface at compile time (sealed `when`) or
  in the serialization test, not in production.

## Enforcement

Sealed hierarchy makes unmapped modules a compile error. A serialization test
asserts every `BngrFeedModule` subtype has a DTO mapping and a `type`
discriminator. ArchUnit rule: providers may live in any domain, but
`feed` itself depends on no other domain (ADR-001).
