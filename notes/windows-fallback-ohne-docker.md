# Fallback: Projekt unter Windows ohne Docker ausführen (nur Windows + Java 21)

Szenario: ein Windows-Rechner mit JDK 21, aber ohne Docker (und ohne die
Rechte, es zu installieren). Ziel: das Projekt zum Laufen bringen. Die App
braucht ein echtes Postgres (Flyway + JPA mit `ddl-auto=validate`), deshalb
ist der Fallback ein **portables Postgres** - braucht weder Admin-Rechte noch
Docker.

## Was unter Windows ohne Änderungen funktioniert

- **`mvnw.cmd`** - das Repo bringt den Maven Wrapper für Windows mit; Maven
  wird nicht installiert.
- Die **schnellen Tests** brauchen kein Docker: `mvnw.cmd test -DskipITs`
  (Unit + Serialisierung + ArchUnit). Nur `mvnw.cmd verify` (Testcontainers)
  setzt Docker voraus - weglassen.
- **`scripts/dev` NICHT verwenden** (ist Bash). Compose natürlich auch nicht.

Zuerst prüfen:

```bat
java -version        :: muss 21.x anzeigen
mvnw.cmd -version    :: nutzt JAVA_HOME; falls Fehler: set "JAVA_HOME=C:\pfad\zum\jdk21"
```

## Option A (empfohlen): portables Postgres, ohne Installation und ohne Admin

1. Die **Binaries als Zip** von PostgreSQL 16 für Windows x64 herunterladen
   (enterprisedb.com -> "Download PostgreSQL binaries", NICHT den Installer).
2. Nach `C:\pgsql` entpacken (oder in einen beliebigen beschreibbaren Ordner).
3. Einen lokalen Cluster initialisieren - mit dem User, den die App erwartet
   (`homefeed`), und Auth `trust` (akzeptiert jedes Passwort - nur für die
   lokale Demo):

   ```bat
   C:\pgsql\bin\initdb -D C:\pgsql\data -U homefeed -E UTF8 -A trust
   ```

4. Postgres starten (lauscht auf localhost:5432, dem Default der App):

   ```bat
   C:\pgsql\bin\pg_ctl -D C:\pgsql\data -l C:\pgsql\log.txt start
   ```

5. Die Datenbank anlegen:

   ```bat
   C:\pgsql\bin\createdb -U homefeed homefeed
   ```

6. Die App aus dem Repo-Root starten - Flyway legt das Schema an und seedet
   die Demo-Daten automatisch:

   ```bat
   mvnw.cmd spring-boot:run
   ```

7. Testen: `http://localhost:8080/scalar` im Browser öffnen, oder:

   ```bat
   curl http://localhost:8080/api/v1/homefeed
   ```

   Demo-Login: `felix.junghans@breuninger.de` / `breuninger-demo`.

Postgres am Ende stoppen:

```bat
C:\pgsql\bin\pg_ctl -D C:\pgsql\data stop
```

## Option B: offizieller PostgreSQL-Installer (falls Admin-Rechte vorhanden)

PostgreSQL 16 mit dem EDB-Installer installieren. Der Superuser heißt dann
`postgres` mit dem gewählten Passwort - die App per Env-Vars darauf zeigen
lassen, statt `application.yml` anzufassen (klassisches cmd; in PowerShell
`$env:VAR="..."`):

```bat
set SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/homefeed
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=dasGewaehltePasswort
mvnw.cmd spring-boot:run
```

(Vorher einmalig die DB anlegen: `createdb -U postgres homefeed` oder über pgAdmin.)

## Warum KEIN Fallback mit H2

Verlockend ("null Installation"), würde aber Code-Änderungen erfordern und
etwas vortäuschen: Die Flyway-Migrationen nutzen Postgres-Typen
(`TIMESTAMPTZ`, `INTERVAL` im Seed), `ddl-auto=validate` würde gegen einen
anderen Dialekt validieren, und ADR-009 sowie der Test-Abschnitt setzen
bewusst auf "echtes Postgres, kein H2". Ein portables Postgres kostet fünf
Minuten und führt exakt denselben Code aus wie die Produktion.

## Kurzfassung

1. Zip mit Postgres-16-Binaries herunterladen -> entpacken.
2. `initdb -D data -U homefeed -A trust` -> `pg_ctl -D data start` -> `createdb -U homefeed homefeed`.
3. Im Repo: `mvnw.cmd spring-boot:run`.
4. `http://localhost:8080/scalar` öffnen.
