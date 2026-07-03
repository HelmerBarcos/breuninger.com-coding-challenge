# breuninger.com-coding-challenge
Coding Challenge: Senior Backend / Infrastructure Engineer (m/w/d) für das INSPO Team // Remote möglich

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
| [ktlint](https://pinterest.github.io/ktlint/) | latest | optional — enables the auto-format hook |

No local Maven or Kotlin needed: the Maven Wrapper (`./mvnw`) and the Kotlin Maven
plugin provide both.
