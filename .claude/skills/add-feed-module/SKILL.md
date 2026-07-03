---
name: add-feed-module
description: Scaffold a new homefeed module type (domain model, ModuleProvider, DTO, serialization registration, tests) following the extension mechanism from ADR-002. Use when asked to add a new module, section, banner, or teaser to the homefeed.
---

# Add Feed Module

Adding a module NEVER modifies `BngrHomefeedService` or any existing provider.
If a step seems to require that, stop and re-read ADR-002 — something is wrong.

## Inputs to collect (ask if not given)

1. Module name (e.g. `RecommendationsModule`)
2. Owning domain package: `catalog`, `orders`, `auth`, or a new domain under
   `com.breuninger.homefeed` (new domain = also check the ArchUnit dependency rules)
3. Payload fields (names + types)
4. Feed position (`order` value — look at existing providers to pick a slot)
5. Protected? (only rendered for authenticated users -> provider returns `null`
   when `ctx.user == null`)

## Steps

1. **Domain model** in `feed/domain`: data class `Bngr<Name>Module` implementing
   the sealed `BngrFeedModule`. Value classes for IDs. No Spring/Jackson/JPA imports.
2. **Provider** in the owning domain package: `Bngr<Name>ModuleProvider` implementing
   `BngrModuleProvider` (`suspend fun provide(ctx): BngrFeedModule?`), annotated
   `@Component`. Simulated latency via the shared latency configuration property,
   never a hardcoded `delay()`.
3. **DTO** in `feed/api`: `Bngr<Name>ModuleDto` implementing the sealed DTO interface;
   mapping added to the `toDto()` extension function (the `when` is exhaustive — the
   compiler will point at it).
4. **Serialization registration**: constant in `BngrModuleTypes` (snake_case) +
   `@JsonSubTypes` entry. These are the only touches to existing files.
5. **Tests**:
   - Unit test for the provider (applies/doesn't apply, payload correctness).
   - Serialization test: DTO emits the right `type` discriminator.
   - If protected: assert absence for anonymous `FeedContext`.
6. **Verify**: `./mvnw -q -DskipITs test` must pass, including the ArchUnit rules.

## Checklist before finishing

- [ ] No existing file modified except `BngrModuleTypes`, the `@JsonSubTypes` list,
      and the `toDto()` mapper
- [ ] All new types carry the `Bngr` prefix
- [ ] Provider returns `null` (not an empty module) when the module doesn't apply
- [ ] README module table updated
