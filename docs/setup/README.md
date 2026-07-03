# Development Environment Setup

Everything needed to develop this project. Instructions are for **macOS** (the
only platform this setup has been tested on); Windows users see the
[Windows via WSL2](#windows-via-wsl2) section. Each section ends with a
verification command — run it before moving on.

## Prerequisites overview

| Tool | Why | Managed by |
|------|-----|------------|
| Docker Desktop | Postgres locally, dev/prod images, Testcontainers | manual install |
| SDKMAN | JDK version manager | manual install (once) |
| JDK 21 (Temurin) | run/build the app on the host | SDKMAN + `.sdkmanrc` |
| VS Code + extensions | IDE, debugging, tasks | `.vscode/extensions.json` |

No local Maven or Kotlin installation is needed: the Maven Wrapper (`./mvnw`)
and the Kotlin Maven plugin provide both.

## 1. Docker

Install Docker Desktop for Mac:

```bash
brew install --cask docker
open -a Docker        # start it once so the daemon is running
```

Verify:

```bash
docker --version && docker compose version
```

## 2. SDKMAN

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"   # or open a new terminal
```

Recommended: enable automatic toolchain switching when entering a directory
with a `.sdkmanrc` (this repo has one). In `~/.sdkman/etc/config` set:

```
sdkman_auto_env=true
```

Verify:

```bash
sdk version
```

## 3. JDK 21 (Temurin)

The repo pins the exact version in [`.sdkmanrc`](../../.sdkmanrc). From the
repo root:

```bash
sdk env install   # installs the pinned java=21.0.7-tem
sdk env           # activates it in this shell (automatic with sdkman_auto_env=true)
```

Verify:

```bash
java -version     # must print: openjdk version "21.0.7" ... Temurin
```

## 4. VS Code

```bash
brew install --cask visual-studio-code
```

Enable the `code` CLI if missing: in VS Code press `Cmd+Shift+P` ->
"Shell Command: Install 'code' command in PATH".

### Recommended extensions

Opening the repo in VS Code prompts to install the recommendations from
[`.vscode/extensions.json`](../../.vscode/extensions.json) (accept the
notification, or Extensions view -> filter `@recommended`). Manually:

```bash
code --install-extension vscjava.vscode-java-pack
code --install-extension mathiasfrohlich.Kotlin
code --install-extension ms-azuretools.vscode-docker
```

## 5. Debugging from VS Code (breakpoints)

Both dev modes support breakpoints; pick the matching launch config (Run and
Debug panel, or F5):

**App on the host** — config **"start:debug (app local, DB in Docker)"**.
Plain `launch`: VS Code starts Postgres first (preLaunchTask `db:up`), runs the
app under its own debugger, breakpoints just work.

**App inside Docker** — config **"attach: app in Docker (5005)"**.
1. `./scripts/dev start:debug --docker` (the dev image starts the JVM with a
   JDWP agent listening on 5005, and compose publishes the port).
2. Once the app is up, run the attach config: VS Code connects to
   `localhost:5005` and breakpoints bind to the sources in your workspace.

Notes:
- The agent uses `suspend=n`: the app boots without waiting for a debugger, so
  code that runs during startup (Flyway, bean init) executes before you can
  attach. Change to `suspend=y` in the Dockerfile CMD if you ever need to debug
  startup itself.
- After editing code in `--docker` mode, recompile (`./mvnw compile` or VS Code
  build on save) — devtools restarts the app in the container and the debugger
  reattaches automatically.

## 6. Optional tooling

```bash
brew install ktlint   # enables the auto-format hook in .claude/hooks/ (no-op without it)
```

## Windows via WSL2

Untested, but this is the supported path. The tooling here is Unix-shaped
(SDKMAN and `scripts/dev` are bash), so on Windows everything runs inside WSL2
rather than natively:

1. **WSL2 + Ubuntu**: `wsl --install -d Ubuntu` in an elevated PowerShell, then reboot.
2. **Docker Desktop for Windows** with the WSL2 backend enabled
   (Settings -> Resources -> WSL integration -> enable for Ubuntu). `docker` is then
   available inside WSL.
3. **Inside WSL**, follow sections 2–3 above verbatim (SDKMAN + JDK 21) — they are
   Linux commands and work unchanged. Clone the repo inside the WSL filesystem
   (`~/...`, not `/mnt/c/...`) or file watching and build performance suffer badly.
4. **VS Code on Windows** + the "WSL" extension (`ms-vscode-remote.remote-wsl`):
   open the repo with `code .` from the WSL shell. Extensions from
   `.vscode/extensions.json` must be installed "in WSL" when prompted.
5. `./scripts/dev` and `./mvnw` work unchanged inside the WSL shell.

Native Windows (PowerShell + winget Temurin) is possible but unsupported here:
SDKMAN doesn't run there and `scripts/dev` would need a PowerShell port.

## Final check

```bash
docker --version && java -version && ./mvnw -version
```

Then start developing — see the workflow commands in [`CLAUDE.md`](../../CLAUDE.md)
or run `./scripts/dev` for usage.
