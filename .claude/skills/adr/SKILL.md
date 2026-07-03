---
name: adr
description: Create a new Architecture Decision Record in docs/adr/ using the house template (context, honest comparison of options in a table, decision, consequences). Use when a significant technical decision is made or the user asks to document a decision.
---

# Create an ADR

House style: an ADR that honestly compares alternatives is worth more than one
that only justifies the winner. Where possible, decisions become *executable*
(an ArchUnit rule, a test, a lint check) — say how at the end of the ADR.

## Steps

1. Determine the next number: list `docs/adr/`, take highest + 1, zero-padded
   to 3 digits. File name: `NNN-kebab-case-title.md`.
2. Write the ADR in English using the template below. Keep it to roughly one
   page. Real trade-offs, no straw men: the rejected options must be described
   at their strongest.
3. If the decision is enforceable, add or extend an ArchUnit/test rule and
   reference it from the ADR.
4. Add a line to the index in `docs/adr/README.md` (create it if missing).

## Template

```markdown
# NNN. <Title>

Date: <YYYY-MM-DD>
Status: Accepted

## Context

<What forces this decision? 2-5 sentences. Link the challenge/position
requirement if relevant.>

## Options considered

| Criterion | Option A | Option B | Option C (chosen) |
|---|---|---|---|
| ... | ... | ... | ... |

<One short paragraph per option, described at its strongest.>

## Decision

<What we chose and the 2-3 reasons that actually tipped the scale.>

## Consequences

<What becomes easier, what becomes harder, what we accept as cost.>

## Enforcement

<ArchUnit rule / test / lint that makes this decision executable, or "manual" if none.>
```
