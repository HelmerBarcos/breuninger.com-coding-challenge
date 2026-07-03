# 011. Content localization: Accept-Language with German default; system messages stay English

Date: 2026-07-03
Status: Accepted

## Context

Module content the server owns (teaser and recommendations headlines) should
reach the client in its language. This is a German shop: the default content
language is German, not English. At the same time ADR-010 fixed all system and
error messages to English - two different audiences (end users vs developers),
two different rules.

## Options considered

| Criterion | A) Spring LocaleResolver (accept-header) drives everything | B) No localization, German hardcoded | C) Explicit content locale in the feed context (chosen) |
|---|---|---|---|
| Error messages stay English (ADR-010) | Broken: validation messages follow the header | Yes | Yes - separate concern, untouched |
| Content per client language | Yes | No | Yes |
| Locale visible in the domain | Hidden in thread-local state | - | Explicit data in `BngrFeedContext` |
| Testability | Global state | - | Pure function + bundle lookup |

**A** couples two unrelated contracts to one resolver: turning on content
negotiation would silently flip validation messages to German. **B** ignores
a real mobile need. **C** treats the content locale as what it is - request
data for the feed.

## Decision

- The feed endpoint reads `Accept-Language` itself and resolves it against the
  supported content locales (`de`, `en`) with a pure function
  (`resolveContentLocale`). Missing, unsupported or malformed headers degrade
  to **German** - a wrong language header never costs the user the feed.
- The resolved `Locale` travels as plain data in `BngrFeedContext`; providers
  look up their texts in the `i18n/feed-content` bundles via a dedicated
  `bngrContentMessageSource` (base bundle = German, `fallbackToSystemLocale`
  off so the server JVM locale never leaks in).
- Spring's LocaleResolver stays `fixed=en`: validation and error messages
  remain English (ADR-010) no matter what the client sends.
- **Campaign texts are data, so they are localized as data**: nullable
  `headline_en`/`cta_label_en` columns on the campaign record (V4), German
  base columns as the fallback. Bundles are for texts the service owns;
  i18n columns are for texts the data owns.
- Not localized on purpose: product and brand names (market-neutral in
  fashion) and the greeting (carries no text at all, ADR-008).

## Consequences

- New content language = one new bundle file + one entry in the supported list.
- Two locale mechanisms coexist by design; the ADR is the place that explains
  why (developer messages vs user content).
- Providers now depend on the content MessageSource - acceptable: they already
  are Spring components at the domain edge.

## Enforcement

`BngrContentLocaleTest` (parameterized) pins the resolution table including
malformed headers. `BngrHomefeedApiIT` asserts German without header, English
with `Accept-Language: en`, German again for unsupported languages, and the
existing English-messages test in `BngrErrorContractIT` proves ADR-010 is
unaffected.
