# 007. Naming: business prefix `Bngr` on all our types

Date: 2026-07-03
Status: Accepted

## Context

When reading stack traces, logs, thread dumps or heap dumps, distinguishing
our classes from framework/library classes at a glance speeds up debugging.
Additionally, generic domain nouns (`User`, `Product`, `Order`) collide with
types from Spring Security, JPA and half the classpath, causing import
ambiguity. A business prefix on our own types addresses both.

## Options considered

| Criterion | A) No prefix (rely on packages/FQCN) | B) Full prefix `Breuninger` | C) Short prefix `Bngr` (chosen) |
|---|---|---|---|
| Stack-trace scanning | Read package columns | Instant | Instant (`Bngr*`) |
| Import collisions (`User`, `Order`, ...) | Frequent, solved per-file with aliases | None | None |
| Name length | Short | `BreuningerProductTeaserModuleProvider` (37 chars) | `BngrProductTeaserModuleProvider` |
| Newcomer clarity | N/A | Self-explanatory | Needs the `Bngr = Breuninger` mapping documented (this ADR) |

The honest counter-argument for **A**: stack traces already contain the FQCN
(`com.breuninger.homefeed...`), so package names identify ownership. We accept
the prefix cost anyway because the *simple* name travels alone in many places —
log patterns, IDE tabs and breadcrumbs, heap-dump histograms, conversation
("look at BngrHomefeedService") — where the package is absent or truncated.
**B** delivers the same benefits but inflates every compound name past
readability.

## Decision

Every class and interface we define starts with `Bngr`:
`BngrFeedModule`, `BngrHomefeedService`, `BngrUser`, `BngrModuleProvider`.

Casing is `Bngr`, **never** `BNGR`: Kotlin/Java conventions capitalize only the
first letter of acronyms longer than two letters (`XmlFormatter`,
`HttpClient`). All-caps breaks CamelCase word boundaries (`BNGRFeedModule`)
and degrades IDE camel-hump search. Mapping for newcomers: **Bngr =
Breuninger**.

## Consequences

- Grep/scan for `Bngr` finds exactly our code, everywhere a simple name
  appears.
- Slightly longer names everywhere; autocompletion absorbs the cost.
- Test classes follow the production class they test
  (`BngrHomefeedServiceTest`).

## Enforcement

ArchUnit rule: every production class under `com.breuninger.homefeed`
(excluding generated code) has a simple name starting with `Bngr`.
