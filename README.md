# Breuninger Homefeed Service

Backend service that composes the homefeed of a mobile shopping app: a REST
endpoint delivering an ordered list of typed modules (greeting, sale banner,
product teaser, recommendations, order history) that the app renders in
sequence. Spring Boot 3.5 + Kotlin + PostgreSQL.

> **Note:** the development setup has only been tested on **macOS**. Windows users
> should follow the [WSL2 path](docs/setup/README.md#windows-via-wsl2) in the setup
> guide (untested).

## Requirements

Install guide with verification steps for each item: [docs/setup](docs/setup/README.md).

| Tool | Version | Notes |
|------|---------|-------|
| [Docker Desktop](https://docs.docker.com/desktop/) | recent (Compose v2) | local Postgres, dev/prod images, Testcontainers |
| [SDKMAN](https://sdkman.io/) | latest | JDK version manager; enable `sdkman_auto_env=true` |
| JDK 21 (Temurin) | pinned in [`.sdkmanrc`](.sdkmanrc) | `sdk env install` from the repo root |
| [VS Code](https://code.visualstudio.com/) | latest | recommended extensions in [`.vscode/extensions.json`](.vscode/extensions.json) |
| [ktlint](https://pinterest.github.io/ktlint/) | latest | optional - enables the auto-format hook |

No local Maven or Kotlin needed: the Maven Wrapper (`./mvnw`) and the Kotlin Maven
plugin provide both.

## Run it

```bash
./scripts/dev start:debug            # Postgres in Docker, app on the host (debug on 5005)
./scripts/dev start:debug --docker   # Postgres + app both in Docker (dev image, debug on 5005)
```

Or in VS Code: **F5** (starts the DB first, then the app under the debugger).
Tests:

```bash
./mvnw test          # unit + serialization + architecture rules (fast, no DB)
./mvnw verify        # adds integration/e2e tests against a Testcontainers Postgres
```

## Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/v1/homefeed` | optional Bearer | The ordered feed. Anonymous: 4 public modules. Authenticated: + `order_history`, personalized greeting. |
| POST | `/api/v1/auth/register` | - | Create a user; 3-5 mock purchases are seeded automatically (there are no purchase endpoints). |
| POST | `/api/v1/auth/login` | - | Exchange credentials for a JWT. |
| GET | `/scalar` | - | Interactive API reference ([Scalar](https://scalar.com) rendering the springdoc OpenAPI spec from `/v3/api-docs`). |
| GET | `/actuator/health/liveness` · `/readiness` | - | Kubernetes probes (readiness includes the DB check, liveness deliberately does not). |

### Demo users (Flyway-seeded, password `breuninger-demo`)

`admin.inspoteam@breuninger.com` (Admin INSPO Team, DIVERSE) ·
`felix.junghans@breuninger.de` (Felix Junghans, MALE) ·
`helmer.barcos@breuninger.de` (Helmer Barcos, MALE)

Explicitly non-production data. Try it:

```bash
TOKEN=$(curl -s localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"felix.junghans@breuninger.de","password":"breuninger-demo"}' | jq -r .accessToken)
curl -s localhost:8080/api/v1/homefeed -H "Authorization: Bearer $TOKEN" | jq '.modules[].type'
```

## Response shape

An envelope object, never a bare array - metadata can grow without breaking clients:

```json
{
  "modules": [
    { "type": "greeting", "firstName": "Felix", "lastName": "Junghans" },
    { "type": "sale_banner", "headline": "Mid-Season Sale - bis zu 30%", "ctaLabel": "Jetzt shoppen", "imageUrl": "..." },
    { "type": "product_teaser", "headline": "Neu bei Breuninger", "products": [ "..." ] },
    { "type": "recommendations", "headline": "Für dich empfohlen", "products": [ "..." ] },
    { "type": "order_history", "purchases": [ "..." ] }
  ],
  "meta": { "generatedAt": "2026-07-03T12:00:00Z", "assemblyTimeMs": 163, "moduleCount": 5 }
}
```

`meta.assemblyTimeMs` makes the concurrency visible: the five providers resolve
in parallel (Kotlin coroutines), each simulating ~150 ms of backend latency -
the feed takes ~max, not ~sum. A failing provider costs its own module, never
the whole feed.

### Content language (`Accept-Language`)

Module content (teaser/recommendations headlines) is localized via the
`Accept-Language` header: `de` (default) and `en` are supported; anything
missing, unsupported or malformed degrades to German (ADR-011). Only content -
error messages stay English (ADR-010), the greeting carries no text (ADR-008),
and campaign texts are data. Translations live in
[`src/main/resources/i18n`](src/main/resources/i18n).

```bash
curl -s localhost:8080/api/v1/homefeed -H "Accept-Language: en" | jq -r '.modules[2].headline'
# New at Breuninger
```

## Errors

Every error is an RFC 9457 problem (`application/problem+json`) extended with an
`errors[]` array - `code` is the client's rendering contract (registry:
`BngrErrorCode`), `message` is for developers, `field` attributes the error, and
one response can carry several errors (ADR-010):

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

The API is deliberately strict - silently ignored input hides client bugs:
unknown body fields -> 400 `UNKNOWN_FIELD`, unknown query parameters -> 400
`UNKNOWN_PARAMETER` (one error per parameter), unknown paths -> 404 `NOT_FOUND`
with the same JSON shape, wrong method -> 405, unparseable JSON -> 400
`MALFORMED_BODY`. The whole contract is pinned by `BngrErrorContractIT`.

## Annahmen

Die bewusst getroffenen Annahmen hinter dem Design:

- **Default-Locale ist Deutsch, nicht Englisch** - es ist ein deutscher Shop.
  Ohne (oder mit nicht unterstütztem) `Accept-Language`-Header kommt der
  Inhalt auf Deutsch (ADR-011). System- und Fehlermeldungen sind davon
  getrennt und immer Englisch (ADR-010).
- **Der Client entscheidet über "Guten Morgen" vs. "Guten Tag"** - abhängig
  von seiner lokalen Uhrzeit und seinem Locale. Der Server kennt beides nicht
  und schickt deshalb nur Identitätsdaten im Greeting (ADR-008).
- **Anonyme Aufrufe sind ein normaler Anwendungsfall**, kein Fehler: der Feed
  antwortet ohne Token mit den öffentlichen Modulen; persönliche Module
  erscheinen nur mit nachgewiesener Identität.
- **Clients überspringen unbekannte Modultypen** - so kann der Feed wachsen,
  ohne alte App-Versionen zu brechen (Forward Compatibility).
- **Käufe sind Snapshots**: Produktname und Preis werden zum Kaufzeitpunkt
  festgehalten, kein Foreign Key in den Katalog - wie echte Bestellpositionen.
- **Ein Markt, ein Feed**: keine Pagination, keine Mandanten - der Feed ist
  kurz genug für eine Antwort.
- **Demo-Daten sind ausdrücklich keine Produktionsdaten** (geteiltes
  Demo-Passwort, In-Memory-Signaturschlüssel - beides dokumentiert).

## Decisions

Architecture decisions live as one-page ADRs in [docs/adr](docs/adr/README.md) -
each one compares the rejected options honestly and states how the decision is
*enforced* (ArchUnit rules, tests). The short version:

- **Package-by-domain, hexagonal only where justified** (ADR-001): `feed/domain`
  is the single framework-free core; `catalog`, `orders` and `auth` plug in by
  implementing `BngrModuleProvider`. Adding a module type = adding classes -
  the feed service is never touched (ADR-002).
- **Jackson polymorphic DTOs** with a snake_case `type` discriminator, separate
  from the domain model (ADR-003).
- **Coroutine fan-out with per-module degradation** (ADR-004).
- **Self-issued JWT validated by Spring's OAuth2 resource server** (ADR-005) -
  swapping to a real IdP (e.g. Cognito) is a config change, not a rewrite.
  Tokens die with the process (in-memory keypair): accepted trade-off.
- **The greeting sends identity data, not "Guten Morgen, …"** (ADR-008): the
  client owns time-of-day and locale; the server stays cacheable.
- **Flyway owns the schema** (`ddl-auto=validate`), seed data is a migration (ADR-009).
- Every class we define carries the `Bngr` prefix (= Breuninger, ADR-007) -
  instantly recognizable frames in stack traces; enforced by ArchUnit.

### Tests - wenige, aber aussagekräftige

Nicht Abdeckung war das Ziel, sondern die Stellen, an denen ein Fehler echten
Schaden anrichtet. Die fünf wichtigsten Tests:

1. **Kein Datenleck an anonyme Clients** (`BngrAuthFlowIT`) - der wichtigste
   Test des Projekts: Ohne gültigen Token enthält der Feed kein
   `order_history`-Modul und keine Namen im Greeting; ein manipulierter Token
   bekommt 401. Personenbezogene Daten verlassen den Server nur mit
   nachgewiesener Identität.
2. **Der komplette Auth-Flow, end-to-end** (`BngrAuthFlowIT`): register ->
   login -> Feed mit Bearer-Token enthält das persönliche Modul mit den
   geseedeten Käufen. Beweist JWT-Ausstellung und -Validierung gegen eine
   echte Postgres (Testcontainers, bewusst kein H2).
3. **Der Serialisierungs-Vertrag** (`BngrFeedModuleSerializationTest`,
   parametrisiert): Jeder Modultyp liefert seinen stabilen
   `type`-Diskriminator - das ist der Vertrag mit der App. Ein Zähler-Test
   vergleicht die Testfälle mit den sealed Subtypes: Ein neues Modul ohne
   Testfall schlägt fehl. Und das Greeting darf ausschließlich
   Identitätsfelder enthalten (ADR-008).
4. **Strikte Eingabe- und Ausgabe-Kontrakte** (`BngrErrorContractIT`,
   `BngrHomefeedApiIT`): Unbekannte JSON-Felder und Query-Parameter werden
   abgelehnt (z. B. beim Anlegen eines Users), mehrere Validierungsfehler
   kommen in einer einzigen Antwort mit maschinenlesbaren Codes - und die
   ausgelieferten DTOs halten das Modell ein: Bild-Links sind absolute
   https-URLs, Namen bleiben unter den Längengrenzen, Preise sind nie negativ.
5. **Degradation statt Ausfall** (`BngrHomefeedServiceTest`, Coroutinen mit
   virtueller Zeit): Wirft ein Provider eine Exception, fehlt nur sein Modul -
   der Feed antwortet trotzdem, und die Reihenfolge bleibt deterministisch,
   unabhängig davon, welcher Provider zuerst fertig wird.

Dazu erzwingen ArchUnit-Tests die Architekturregeln (Abhängigkeitsrichtung,
`Bngr`-Namenskonvention) bei jedem Build. Bewusst nicht getestet:
JPA-Repositories ohne eigene Logik und der Latenz-Simulator.

## Docker

One multi-stage [`Dockerfile`](Dockerfile) (ADR-006), two consumable targets:

- **dev** - `maven:3.9-eclipse-temurin-21`, JDWP debugger on 5005, used by
  compose with `src/` and `target/` mounted. Auto-reload works via
  spring-boot-devtools: it restarts when recompiled classes land in `target/`
  (VS Code compiles on save; otherwise run `./mvnw compile`). Honest mechanism,
  no magic.
- **prod** - distroless Java 21, `nonroot` (uid 65532), no shell/JDK tooling,
  Spring Boot layered-jar COPY order so a code change re-pushes KBs, not MBs.

Multi-arch (amd64 + arm64): `docker buildx bake`.

## Wie würde ich diesen Service in einer Cloud-/Kubernetes-Umgebung betreiben?

- **Traffic path & scaling**: a Deployment with N replicas behind a Kubernetes
  **Service** that load-balances across all pods, an **Ingress** in front, and an
  **HPA** scaling on actual traffic (requests/sec via the Prometheus adapter, not
  just CPU). The service is stateless (JWT, no sessions), so scaling out is
  safe - with one honest caveat: today's in-memory signing keypair (ADR-005)
  means a token issued by pod A would not validate on pod B. Before going
  beyond one replica, token issuing moves to a shared key or a real IdP -
  exactly the migration path ADR-005 planned.
- **Probes**: liveness = `/actuator/health/liveness` (no DB check - restarting a
  pod doesn't fix a down database); readiness = `/actuator/health/readiness`
  (includes the DB -> pod leaves the Service instead of crash-looping).
- **Secrets**: never plain values in manifests - a dedicated secret manager
  (**OpenBao**/Vault, or AWS Secrets Manager) synced into the cluster via the
  External Secrets Operator. The app stays unchanged: it only reads env vars
  (12-factor); rotation happens in the manager. In AWS: RDS Postgres + IRSA
  instead of static DB credentials, JWT validation pointed at Cognito's JWKS.
- **Pod hardening**: the pod gets only the permissions it needs -
  `runAsNonRoot` + `readOnlyRootFilesystem` + `capabilities: drop: [ALL]` +
  seccomp `RuntimeDefault` (the distroless non-root image passes the
  *restricted* PodSecurity standard as-is); a dedicated ServiceAccount with
  `automountServiceAccountToken: false`; a NetworkPolicy allowing egress only
  to Postgres and DNS.
- **TLS**: **cert-manager** issues and auto-renews the certificates at the
  Ingress (ACME/Let's Encrypt or a corporate CA) - no manual certificate
  handling, no expiry incidents.
- **Caching**: the anonymous feed is identical for everyone and carries no
  time-dependent text (ADR-008), so it is cacheable at the edge with a short TTL.
- **Graceful shutdown** is enabled (`server.shutdown=graceful`) so rolling
  deployments drain in-flight requests; resources requests/limits sized from
  the Micrometer metrics Actuator already exposes (Prometheus scrape).
- **Images**: multi-arch tags (`docker buildx bake`) let the same manifest run
  on Graviton or x86 node groups.

## Consciously left out / next steps

- Refresh tokens, roles, logout, key rotation (ADR-005 documents the IdP path).
- Response caching / ETags for the anonymous feed.
- Module personalization beyond the mocked recommendations slice.
- CI pipeline (the build is one `./mvnw verify` + `docker buildx bake` away).
- Structured JSON logging + tracing (Micrometer Tracing) for production.

### Was ich in einem echten Produkt anders machen würde

- **Flyway-Migrationen als eigenen Schritt ausführen**, nicht beim App-Start:
  aus der CI/CD-Pipeline heraus, oder bei Continuous Delivery mit Argo CD als
  eigene Sync-Wave *vor* dem Deployment der Dienste. Schlägt die Migration
  fehl, werden die Dienste gar nicht erst aktualisiert.
- **Eine Rule Engine für komplexere Geschäftsregeln** dazuschalten, statt die
  Regeln in den Service-Code zu schreiben. Klassisches Modell: **Facts** sind
  die Informationen zur Bewertung (aus der DB oder von Drittsystemen),
  **Toggles** schalten einzelne Regeln an oder aus (wichtig z. B. für
  Premium-Kunden), und der **Output** ist eine schreibgeschützte Map, deren
  Keys die Domäne abbilden und deren Values das Ergebnis der Engine sind.
- **SonarQube aktivieren**, um potenzielle Schwachstellen und Code-Smells
  automatisch zu erkennen.
- **Einen strengeren Linter/Formatter erzwingen** (z. B. ktfmt im Google-Style
  oder detekt): einheitliches Aussehen des Codes, Grenzen für Funktionslänge
  und Komplexität - nicht verhandelbar im Review, weil die Maschine es prüft.
- **Eine CI-Pipeline**, die bei jedem Commit Build und alle Tests laufen lässt
  (`./mvnw verify`) - ohne grünen Build kein Merge.
- **OPA (Open Policy Agent) für Policy-as-Code**: Berechtigungen prüft die
  Infrastruktur, nicht die Anwendung. So bleiben die Dienste so schlank wie
  möglich, und Policies sind versioniert, testbar und zentral auditierbar.
- **Response-Header in Produktion prüfen** und alles abschalten, was Versionen
  oder eingesetzte Technologien verrät - weniger Angriffsfläche.
- **Das neue HTTP-Verb `QUERY` evaluieren**: komplexe Abfragen mit Body statt
  überlanger GET-URLs, ohne die Semantik von POST zu missbrauchen.
- **Monitoring und Alerting auf Antwortzeiten ausbauen** (Micrometer liefert
  die Metriken schon): Alerts auf p95/p99-Latenz, damit Fälle sofort
  auffallen, in denen das API zu lange für eine Antwort braucht.
