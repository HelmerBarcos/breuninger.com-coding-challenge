# 008. Greeting module: the API sends identity data, not the composed salutation

Date: 2026-07-03
Status: Accepted

## Context

The greeting module shows a time-of-day salutation like "Guten Morgen, Anna".
A time-of-day greeting depends on the *client's* local time and locale, which
the server does not reliably know: users travel across timezones, device
locale drives the language, and a response body containing "Guten Morgen"
becomes stale at 12:00 and breaks any response caching.

## Options considered

| Criterion | A) Server composes from server time | B) Server composes from a client-sent timezone/locale header | C) Client composes; API sends identity data (chosen) |
|---|---|---|---|
| Correct across timezones | No | Yes, if the header is always sent | Yes |
| Localization | Server must own translations | Server must own translations | Client already owns them |
| Cacheability of the feed | Poor (time-dependent body) | Poor (varies per header) | Good |
| Contract complexity | Lowest | Extra request contract | Structured payload |

**A** is simply wrong for a mobile audience. **B** can be made correct but
moves presentation concerns (translations, time buckets) to the server and
makes responses vary per header. **C** follows the general rule this API
adheres to: the server delivers stable, cacheable *data*; presentation and
localization belong to the client.

## Decision

The greeting payload carries identity data only:

```json
{ "type": "greeting", "firstName": "Helmer", "lastName": "Barcos" }
```

Both fields are `null` for anonymous requests (the app renders a generic
greeting). The client composes the final string - salutation bucket from its
clock, language from its locale, name from the payload. The user model also
carries `gender` (`MALE | FEMALE | DIVERSE`), exposed in the user DTO, so
clients can render gendered salutations where the locale requires them.

## Consequences

- The feed response contains no time- or locale-dependent text, so it stays
  cacheable and testable with exact assertions.
- Salutation wording changes ship with the app, not the backend.
- The DTO carries a code comment stating why no salutation text is sent, so
  the decision survives at the point of temptation.

## Enforcement

Serialization test asserts the greeting payload contains only identity fields
(no salutation/title text). Code comment on the greeting DTO references this
ADR.
