# 006. Docker: one multi-stage Dockerfile, distroless prod, bake for multi-arch

Date: 2026-07-03
Status: Accepted

## Context

Container images are a first-class deliverable of this service, not an
afterthought. Requirements set for this repo: a dev image with full JDK
tooling and debugger for `docker compose` workflows; a prod image that is
visibly smaller, has no JDK dev tools, and runs as non-root; multi-platform
builds (Apple Silicon dev machines ↔ x86 clusters).

## Options considered

| Criterion | A) Single image (JDK, run as root) | B) Buildpacks / Jib (no Dockerfile) | C) Multi-stage Dockerfile: maven dev target + distroless prod target (chosen) |
|---|---|---|---|
| Image size (prod) | ~700MB+ | ~250–300MB | ~250MB, no shell/package manager at all |
| Dev workflow (hot reload, debugger) | Same bloated image everywhere | Separate mechanism needed | `dev` target with maven + JDWP 5005 |
| Attack surface | Shell, JDK tools, root | Small | Minimal: distroless, `nonroot` (uid 65532) |
| Build decisions explicit/reviewable | No | Hidden behind tooling | Yes — the Dockerfile is readable evidence |
| Multi-arch | Manual | Supported | `docker-bake.hcl` targets amd64+arm64 |

**B** is a legitimate production choice, but it hides the very layer this
repository wants to keep explicit. **C** makes every decision visible and
reviewable.

## Decision

One `Dockerfile`, four stages: `deps` (cached `dependency:go-offline`),
`dev` (maven + full JDK + JDWP on 5005, used by compose with the project
mounted), `build` (packages and explodes the Spring Boot layered jar), and
`prod` — `gcr.io/distroless/java21-debian12:nonroot`, layered-jar COPY order
(dependencies before application classes, so code changes re-push only KBs),
`USER nonroot`. `docker-bake.hcl` builds `prod` for `linux/amd64` and
`linux/arm64`; `dev` builds only for the local platform.

## Consequences

- Prod has no shell: no `docker exec` debugging — diagnostics go through
  Actuator, logs and metrics (which is the correct production posture anyway).
- Image sizes are measured and documented in the README as evidence.
- The dev target depends on volume mounts + devtools for reload; that
  mechanism is documented honestly (recompile triggers the restart).

## Enforcement

CI (or a documented manual check): `docker buildx bake` succeeds for both
platforms; `docker inspect` shows non-root user; prod image size recorded in
the README. Container runs with a read-only root filesystem where possible.
