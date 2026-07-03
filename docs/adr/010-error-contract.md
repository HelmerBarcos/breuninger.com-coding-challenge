# 010. Error contract: RFC 9457 extended with typed error codes, strict request validation

Date: 2026-07-03
Status: Accepted

## Context

The mobile app must render error UI from responses without parsing prose: it
needs a machine-readable code, the HTTP status, and a brief server explanation
- and a single request can fail for several reasons at once (e.g. two invalid
form fields). Separately, a lenient API hides client bugs: a misspelled query
parameter that is silently ignored "works" and does nothing, and an unknown
body field is dropped without anyone noticing.

## Options considered

| Criterion | A) Plain RFC 9457 ProblemDetail | B) Fully custom error JSON | C) RFC 9457 extended with `errors[]` of typed codes (chosen) |
|---|---|---|---|
| Client renders by | Parsing `detail` prose (fragile) | Custom code field | Stable `errors[].code` enum |
| Multiple errors per response | No (single detail) | Yes | Yes |
| Standard media type / tooling | `application/problem+json` | Lost | Kept - RFC 9457 allows extension members |
| Infrastructure interop (gateways, logging) | Good | None | Good |

**A** is standard but mono-error and human-oriented. **B** solves the client's
problem but abandons a standard for no reason. **C** keeps `status` and
`detail` (the brief server explanation) from the RFC and adds the extension
member the RFC explicitly permits.

## Decision

Every error response is a ProblemDetail extended with `errors[]`, each entry a
`BngrHttpError`:

```json
{
  "status": 400,
  "detail": "Request validation failed",
  "errors": [
    { "code": "FIELD_INVALID", "message": "must be a well-formed email address", "field": "email" },
    { "code": "FIELD_INVALID", "message": "size must be between 8 and 100", "field": "password" }
  ]
}
```

- `code` comes from the single registry `BngrErrorCode` (shared) - the client's
  contract; messages are for developers and may change freely.
- `field` attributes an error to a request field/parameter when possible.
- Domain handlers (auth) reuse the same shape via `bngrProblemOf(...)`.

**Strict request validation**, all answering in this shape:

- Unknown JSON body fields -> 400 `UNKNOWN_FIELD` (Jackson
  `fail-on-unknown-properties: true`).
- Unknown query parameters -> 400 `UNKNOWN_PARAMETER`, one error per offending
  parameter (`BngrUnknownParameterInterceptor` compares the request against the
  handler's declared `@RequestParam`s; scoped to `/api/**`).
- Unknown paths -> 404 `NOT_FOUND` with the JSON body. The security chain lets
  unmatched paths fall through to MVC (`anyRequest().permitAll()` after gating
  `/actuator/**`) so clients get an honest 404 instead of a misleading 401.
- Wrong method -> 405, wrong content type -> 415, unparseable JSON -> 400
  `MALFORMED_BODY`, invalid enum value -> 400 `FIELD_INVALID` pointing at the field.
- Anything unhandled -> 500 `INTERNAL_ERROR` with no internals leaked.

## Consequences

- Adding an error case means adding a `BngrErrorCode` constant - the registry
  doubles as client-facing documentation.
- Strictness is a behavior change lever: clients get fast feedback on typos,
  but adding a parameter to an endpoint later requires declaring it (that is
  the point).
- One more moving part (the interceptor) - covered by the error-contract IT.

## Enforcement

`BngrErrorContractIT` pins the shape and every strictness rule, including the
multi-error responses and the `application/problem+json` media type.
