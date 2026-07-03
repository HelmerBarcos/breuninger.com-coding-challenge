# 001. Architecture style: package-by-domain with selective hexagonal

Date: 2026-07-03
Status: Accepted

## Context

The service composes a homefeed from heterogeneous module types, and new module
types must be easy to add later. The service is small (one endpoint, a handful
of domains) but the structure must show how it scales. The surrounding platform
is vertically sliced into independently owned services, so a path from packages
to separate services is a real concern.

## Options considered

| Criterion | A) Technical 3-layer (`controllers/`, `services/`, `repositories/`) | B) Full hexagonal (ports & adapters everywhere) | C) Package-by-domain + selective hexagonal (chosen) |
|---|---|---|---|
| Cohesion | Low: one feature spread over 3+ packages | Medium: grouped by architectural role | High: everything about `auth` lives in `auth/` |
| Ceremony per feature | Minimal | High: port + adapter + mapping for everything, even trivial CRUD | Proportional: hexagonal only where there is logic to protect |
| Module extensibility | Weak: the mechanism gets diffused | Strong | Strong: the feed contract is the single hexagon |
| Path to microservices | Rewrite | Reasonable | Direct: each domain package is a service candidate |
| Main risk | Big ball of mud with apparent order | Over-engineering for a service this size; mapping fatigue | Requires discipline in inter-domain dependency rules |

**A** is the tutorial default: simple, but a feature touches every package and
the extension mechanism has no obvious home. **B** applies dependency inversion
uniformly - clean, but most of this service is pass-through CRUD where ports add
cost without protecting anything. **C** groups code by business domain
(`feed/`, `catalog/`, `orders/`, `auth/`, `shared/`) with light internal
separation, and reserves the full hexagonal treatment for the one place the
domain justifies it: the feed assembly and its extension contract.

## Decision

Option C. `feed/domain` is the only framework-free core (no Spring, Jackson, or
JPA imports) and contains the sealed `BngrFeedModule`, the `BngrModuleProvider`
port and `BngrHomefeedService`. Other domains plug in by implementing
`BngrModuleProvider`; they depend on `feed/domain`, never on each other (sole
declared exception: `auth -> orders`, to mock purchases at registration). Domain
packages use Spring Data repositories directly - no ports where there is no
logic to protect.

## Consequences

- Adding a domain or module never touches the feed core; each domain is a
  future service candidate.
- The dependency rules are the load-bearing wall: they must be enforced, not
  just documented.
- The architecture stays proportional to the problem instead of applying one
  pattern uniformly.

## Enforcement

ArchUnit tests: (1) no class in `feed.domain` imports Spring/Jackson/JPA;
(2) `catalog`/`orders`/`auth` do not depend on each other except `auth -> orders`;
(3) only `feed` may be depended on by other domains.
