# Architecture Decision Records

Index of accepted ADRs. Create new ones with the `adr` skill (house template:
context, honest comparison of options, decision, consequences, enforcement).

| # | Title | Status |
|---|-------|--------|
| [001](001-architecture-style.md) | Architecture style: package-by-domain with selective hexagonal | Accepted |
| [002](002-module-extensibility.md) | Feed module extensibility: sealed type + ModuleProvider discovery | Accepted |
| [003](003-serialization-strategy.md) | Serialization: Jackson polymorphic DTOs with a type discriminator | Accepted |
| [004](004-concurrency-model.md) | Concurrency: coroutine fan-out with per-module degradation | Accepted |
| [005](005-authentication.md) | Authentication: self-issued JWT validated by Spring's resource server | Accepted |
| [006](006-docker-images.md) | Docker: one multi-stage Dockerfile, distroless prod, bake for multi-arch | Accepted |
| [007](007-naming-conventions.md) | Naming: business prefix `Bngr` on all our types | Accepted |
