# 009. Database migrations and seed data with Flyway (SQL-first)

Date: 2026-07-03
Status: Accepted

## Context

The service owns a PostgreSQL schema and needs demo/reference data at startup:
a product catalog, an active sale campaign, and three demo users (with mock
purchases, keeping the invariant that every user has purchases). Schema and
seed must be versioned, reproducible across environments (local compose,
Testcontainers, production) and independent of the ORM.

## Options considered

| Criterion | A) Hibernate `ddl-auto` + data.sql | B) Liquibase | C) Flyway, SQL-first (chosen) |
|---|---|---|---|
| Schema ownership | ORM-derived, drifts silently | Migration tool | Migration tool |
| Migration format | — | XML/YAML/JSON abstraction (SQL possible) | Plain versioned SQL |
| Rollback tooling | None | Built-in (mostly paid value in practice) | Forward-only (undo is commercial) |
| Readability for reviewers | Hidden | Indirection through changelog DSL | `V1__schema.sql` reads as what it is |
| Spring Boot integration | Trivial | Auto-config | Auto-config, runs on startup |

**A** is fine for prototypes but the schema becomes an artifact of entity
annotations — unreviewable and unrepeatable. **B** is a legitimate enterprise
choice (database-agnostic changelogs, preconditions); its abstraction pays off
when targeting multiple RDBMS, which this service does not. **C** keeps
migrations as plain SQL against the one database we actually run.

## Decision

Flyway with SQL migrations under `db/migration`:

- `V1__schema.sql` — tables (users, products, sale_campaigns, purchases).
- `V2__seed_catalog.sql` — products and the active sale campaign.
- `V3__seed_users.sql` — demo users with BCrypt password hashes and their
  mock purchases: `admin.inspoteam@breuninger.com` (Admin INSPO Team, DIVERSE),
  `felix.junghans@breuninger.de` (Felix Junghans, MALE),
  `helmer.barcos@breuninger.de` (Helmer Barcos, MALE) — the three legal gender
  values stay representable and exercised.

Hibernate is set to `ddl-auto=validate`: Flyway owns the schema, the ORM only
verifies it matches the entities. Flyway runs automatically at application
startup (Spring Boot auto-configuration), so compose, Testcontainers and any
fresh database converge to the same state with zero manual steps.

## Consequences

- Demo credentials live in a migration; the shared demo password is documented
  in the README (explicitly non-production data).
- Forward-only migrations: fixing a bad migration means a new version, not an
  edit — matching how deployed environments behave anyway.
- Integration tests double as migration tests: every Testcontainers run
  applies the full history from scratch.

## Enforcement

`ddl-auto=validate` fails startup on schema/entity drift. Testcontainers
integration tests boot the full migration chain on a clean PostgreSQL and
assert the seed invariants (3 users, each with purchases).
