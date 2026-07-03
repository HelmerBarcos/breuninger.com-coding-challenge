# Breuninger Homefeed Service

Spring Boot (Kotlin) homefeed service for a mobile shopping app — a coding
challenge for a Senior Backend/Infrastructure role. Decisions are recorded in
`docs/adr/`; working notes in `sessions/` (personal, not part of the delivery).

## Language & commits

- Code, commits, docs, ADRs: **English**. Conventional Commits (`feat:`, `fix:`, `test:`,
  `docs:`, `chore:`, `refactor:`) with an optional scope, e.g. `feat(feed): ...`.
- Commits are authored solely by the local git user — never add `Co-Authored-By` or any
  AI-attribution trailer (also enforced via `attribution` in `.claude/settings.json`).
- Commit history must tell the prioritization story: core challenge first, extensions after.

## Toolchain

- JDK 21 (Temurin) managed via SDKMAN, pinned in `.sdkmanrc` — run `sdk env` (or
  `sdk env install` the first time). No local Maven/Kotlin needed: the wrapper and
  the Kotlin Maven plugin provide them.
- Full environment setup guide (Docker, SDKMAN, VS Code): `docs/setup/README.md`.

## Commands

- `./mvnw -q -DskipITs test` — fast unit tests (default check)
- `./mvnw verify` — includes `*IT` integration/e2e tests via Failsafe (needs Docker for Testcontainers)
- `./scripts/dev start:debug` — Postgres in Docker (waits until healthy), app on the host, debug on 5005
- `./scripts/dev start:debug --docker` — Postgres + app both in Docker (dev image, auto-reload via devtools)
- `./scripts/dev db:up` / `./scripts/dev stop` — just the DB / stop the stack
- `docker buildx bake` — multi-arch images (targets: `dev`, `prod`)
- VS Code: F5 uses `.vscode/launch.json` (preLaunchTask `db:up` guarantees the DB is up first)

## Architecture (ADR-001: package-by-domain, hexagonal only where justified)

- `feed/` is the only hexagon. `feed/domain` must NOT import Spring, Jackson, or JPA.
- `catalog/`, `orders/`, `auth/` plug into the feed by implementing `BngrModuleProvider`.
  They depend on `feed/domain`, never on each other. Sole allowed exception: `auth -> orders`.
- Domain packages use Spring Data repositories directly — no ports there.
- ArchUnit tests enforce these rules. If a rule blocks you, stop and ask — never relax the test.

## Conventions

- Every class/interface we define starts with the prefix `Bngr` (= Breuninger, ADR-007).
  Never `BNGR` — acronym casing follows Kotlin conventions.
- Constructor injection only. Immutability by default (`val`, data classes).
  `@JvmInline value class` for IDs.
- No magic strings/numbers: type discriminators live in `BngrModuleTypes` (snake_case);
  tunables (module order, simulated latency) are typed `@ConfigurationProperties`.
- DTOs are separate from domain models; mapping via explicit extension functions
  (`toDto()`), no mapping libraries.
- Serialization: Jackson polymorphic with `type` discriminator (ADR-003).
  API responses are envelope objects, never bare arrays.
- Errors (ADR-010): RFC 9457 `ProblemDetail` extended with `errors[]` of `BngrHttpError`
  (`code` from the `BngrErrorCode` registry, `message`, optional `field`) — build with
  `bngrProblemOf(...)`. The API is strict: unknown body fields, query params and paths
  are rejected in this shape. New error case = new `BngrErrorCode` constant + assertion
  in `BngrErrorContractIT`. Endpoints under `/api/v1/`.

## Adding a feed module

Use the `add-feed-module` skill. New module = new classes only; the only existing
files touched are `BngrModuleTypes`, the `@JsonSubTypes` list, and the `toDto()`
mapper. Never modify `BngrHomefeedService`.

## Recording decisions

Use the `adr` skill for significant technical decisions: honest comparison table,
and make the decision executable (ArchUnit/test) whenever possible.
