# 005. Authentication: self-issued JWT validated by Spring's resource server

Date: 2026-07-03
Status: Accepted

## Context

The e2e scenario requires a protected feed module ("your last purchases"):
users register, log in, and the same feed endpoint returns an additional
module when called with a valid token. Registration auto-seeds mock purchases;
there are no purchase endpoints. Auth is a supporting capability, not the core
domain - it must be small but production-shaped.

## Options considered

| Criterion | A) Session-based (form login) | B) Custom JWT filter + jjwt library | C) Self-issued JWT + `oauth2-resource-server` (chosen) |
|---|---|---|---|
| Fits stateless mobile API | Poor (cookies, CSRF) | Good | Good |
| Extra dependencies | None | jjwt | None (Nimbus ships with the starter) |
| Custom security code | Little | Filter + parsing + error handling (easy to get wrong) | Token issuing only; validation is framework-standard |
| Path to a real IdP (e.g. Cognito - platform is AWS) | Rewrite | Rewrite of validation | Change the issuer/JWKS config |

**A** contradicts a mobile JSON API. **B** is the common tutorial route but
hand-rolls exactly the code (validation, expiry, error responses) that Spring
already ships hardened. **C** uses Spring's `JwtEncoder`/`JwtDecoder` (Nimbus)
with a keypair generated at startup: the service *issues* tokens like a mini
IdP and *validates* them as a standard OAuth2 resource server.

## Decision

- `POST /api/v1/auth/register` creates the user (BCrypt password) and seeds
  3-5 mock purchases in the same use case.
- `POST /api/v1/auth/login` returns a short-lived JWT signed with an RSA key
  generated at startup.
- The feed endpoint stays `permitAll`: authentication is *optional* there.
  The security layer only populates `BngrFeedContext.user`; each provider
  decides (ADR-002) - `BngrOrderHistoryModuleProvider` returns `null` for
  anonymous requests. No duplicated public/private routes.
- Out of scope, documented in the README: refresh tokens, roles, logout,
  key rotation.

## Consequences

- Swapping to a real IdP later means deleting the issuing code and pointing
  the resource server at an external JWKS - validation code is untouched.
- Tokens die with the process (in-memory keypair) - an accepted trade-off at
  this stage, stated explicitly in the README.
- The e2e test can exercise the full flow with zero external dependencies.

## Enforcement

E2e tests (Testcontainers): anonymous feed lacks `order_history`; authenticated
feed contains it with the seeded purchases; a tampered token yields the
anonymous feed (or 401 - asserted either way).
