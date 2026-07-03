# 003. Serialization: Jackson polymorphic DTOs with a type discriminator

Date: 2026-07-03
Status: Accepted

## Context

The mobile app must recognize each module type from the JSON via a type
discriminator, and the wire format is a long-lived contract with mobile
clients. Kotlin offers its own serialization framework, so the choice is not
automatic.

## Options considered

| Criterion | A) kotlinx.serialization | B) Domain objects serialized directly with Jackson | C) Jackson + dedicated DTO layer (chosen) |
|---|---|---|---|
| Sealed-class polymorphism | Built-in, discriminator for free | Needs annotations on domain types | `@JsonTypeInfo`/`@JsonSubTypes` on DTOs only |
| Reflection | None (compile-time serializers) | Runtime | Runtime (mitigated by jackson-module-kotlin) |
| springdoc/OpenAPI integration | Poor — spec quality collapses | Good | Good: `oneOf` + discriminator generated |
| Ecosystem consistency (ProblemDetail, Actuator) | Second serializer in one API | Single serializer | Single serializer |
| API/domain coupling | — | High: renaming a field breaks the contract | None: DTOs are the contract |

**A** is genuinely attractive — sealed interfaces serialize polymorphically
without annotations — but springdoc inspects Jackson, and Actuator/ProblemDetail
already serialize through Jackson, so choosing A means either two serializers in
one API or losing generated OpenAPI quality. **B** is the least code but welds
the wire format to the domain model. **C** costs one explicit mapping layer and
buys a stable contract plus full ecosystem compatibility.

## Decision

Jackson with `jackson-module-kotlin`. A sealed `BngrFeedModuleDto` hierarchy
annotated with `@JsonTypeInfo(use = NAME, property = "type")`; discriminator
values are snake_case constants centralized in `BngrModuleTypes`. Domain →
DTO mapping via explicit `toDto()` extension functions (exhaustive `when`).
The endpoint returns an envelope object — `{ "modules": [...], "meta": {...} }`
— never a bare array, so metadata can be added without breaking clients.
Convention: `type` values snake_case (stable contract), field names camelCase.

## Consequences

- One mapping function per module type — mechanical, part of the
  `add-feed-module` recipe.
- OpenAPI shows `oneOf` with a discriminator, which is exactly what a mobile
  team consumes.
- kotlinx.serialization remains a documented, viable alternative if the
  service ever drops springdoc and Spring's Jackson-based infrastructure.

## Enforcement

Serialization tests (`@JsonTest`): each DTO emits its `type` discriminator; a
snapshot test of the full envelope documents the contract. The exhaustive
`when` in `toDto()` breaks compilation when a module lacks a mapping.
