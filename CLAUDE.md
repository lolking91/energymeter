# CLAUDE.md – Energy Meter Project

## Purpose

Home energy monitoring system covering both **production** and **consumption**:

- **Production:** Fox ESS M1-800-E microinverter (Balkonkraftwerk)
- **Consumption:** 4 Shelly devices
  - `solar` – Shelly PM Mini (Gen3, single-phase) – metering on the inverter feed
  - `house-total` – Shelly Pro 3EM 120A – whole-house consumption (3-phase)
  - `upper-unit` – Shelly Pro 3EM 120A – upper apartment consumption (3-phase)
  - `lower-unit` – Shelly Pro 3EM 120A – lower apartment consumption (3-phase)

Polls all sources on a schedule, persists readings to InfluxDB 3, and
visualises them in Grafana.

> **Status:** Only Fox ESS and the Shelly PM Mini (`solar`) are implemented so
> far. The 3x Shelly Pro 3EM devices are planned but not yet wired up —
> implement as a new `type` in `ShellyProperties`/`ShellyService` once the
> Pro 3EM's RPC payload (likely `EM.GetStatus`, 3-phase) has been verified
> against a real device.

## Tech Stack

| Layer       | Technology                                  |
|-------------|---------------------------------------------|
| Backend     | Java 21, Spring Boot 4.1.0, Maven           |
| Storage     | InfluxDB 3 Core (self-hosted, single-node)  |
| Dashboards  | Grafana                                     |
| Deployment  | Docker Compose on Raspberry Pi 4 (ARM64)    |

## Project Layout

```
/                     – Spring Boot application (Maven root)
  src/main/java/de/energymeter/
    config/           – @ConfigurationProperties beans, Spring wiring
    foxess/           – Fox ESS API client, DTOs, service
    shelly/           – Shelly local RPC client, DTOs, service
    influx/           – InfluxDB write service
    scheduler/        – @Scheduled polling tasks (one per data source)
    health/           – GET /api/health diagnostic endpoint
  src/main/resources/
    application.yml   – all config with ${ENV_VAR:default} placeholders
  Dockerfile          – multi-stage Maven + JRE build (to be added)
docker-compose.yml    – InfluxDB 3, Grafana, Backend (to be added)
.env.example          – secret template (never commit a real .env)
docs/SETUP.md         – setup and deployment guide (to be added)
```

## Package & Naming Conventions

- **Group ID / base package:** `de.energymeter`
- JavaDoc on every class and every public method (project rule, not optional)
- inline comments with `// ` at important places
- English for all code, comments, commit messages, and documentation

## Secrets Management

All credentials are injected exclusively via environment variables.
**Never hard-code or commit secrets.** The canonical list of required variables
lives in `.env.example`.

| Variable                          | Purpose                          |
|-----------------------------------|----------------------------------|
| `FOXESS_API_KEY`                  | Fox ESS Open API key             |
| `FOXESS_DEVICE_SN`                | Inverter serial number           |
| `SHELLY_SOLAR_HOST`               | IP/hostname of the Shelly PM Mini |
| `INFLUXDB_TOKEN`                  | InfluxDB admin token             |
| `DOCKER_INFLUXDB_INIT_*`          | InfluxDB first-run setup         |
| `GF_SECURITY_ADMIN_PASSWORD`      | Grafana admin password           |

Shelly devices are polled on the local network without authentication — no
secret is needed beyond the device's IP/hostname.

## Fox ESS Open API

- **Feature flag:** `foxess.enabled` (env `FOXESS_ENABLED`, default `true`) —
  set to `false` to disable the Fox ESS scheduler entirely, e.g. while only
  the Shelly integration is in use. `api-key`/`device-sn` default to empty
  strings so the app still starts without them when disabled.
- **Base URL:** `https://www.foxesscloud.com`
- **Auth:** MD5 signature per request – see `FoxEssClient.buildSignature()`
  Formula: `MD5("token={apiKey}&path={path}&timestamp={epoch_ms}&lang=en")`
- **Real-time endpoint:** `POST /op/v0/device/real/query`
- **Key variables polled:** `pvPower`, `generationPower`, `feedInPower`,
  `gridConsumptionPower`, `loadsPower`, `SoC`
- Fox ESS refreshes device data roughly every 5 minutes; the default polling
  interval (`SCHEDULER_INTERVAL_MS=300000`) matches this cadence.

## Shelly API

- Local, unauthenticated RPC API (Gen2/Gen3) — no cloud dependency
- **PM Mini (Gen3, single-phase, implemented):** `GET http://<host>/rpc/PM1.GetStatus?id=0`
  → `apower` (W), `voltage` (V), `current` (A), `freq` (Hz), `aenergy.total` (Wh)
- **Pro 3EM 120A (3-phase, not yet implemented):** expected to use
  `GET http://<host>/rpc/EM.GetStatus?id=0` (per-phase + total active power) —
  verify exact response shape against a real device before implementing
- Devices are configured as a list under `shelly.devices` in `application.yml`
  (`name`, `host`, `type`); `ShellyService` filters by `type` per supported
  device kind

## InfluxDB 3 Data Model & Downsampling

- **Measurement:** `energy` (shared by Fox ESS and Shelly writes)
- **Tag:** `device` = logical device name (`solar`, inverter serial number, etc.)
- **Fields:** one per variable (e.g. `pvPower`, `power`, `voltage`, `energyTotal`)
- **Timestamp precision:** milliseconds (set client-side at write time)

InfluxDB 3 has no Flux/continuous queries (removed in the v2→v3 rewrite).
Downsampling design:

- **Raw database** (`energy_raw`, default retention target: **3 years**) —
  written directly by this application at full polling resolution
- **Downsampled database** (`energy_downsampled`, long-term) — populated by
  the InfluxDB 3 **Processing Engine's Downsampler Plugin**, running
  server-side on a schedule, aggregating raw points into **hourly
  avg/min/max** values
- This application **only ever writes to the raw database** — downsampling
  is entirely a database-side concern, not implemented in Java.

### One-time InfluxDB 3 setup (after first `docker compose up`)

The `influxdb3-core` image has no `DOCKER_INFLUXDB_INIT_*` env vars like v2 —
admin token, databases and the downsample trigger must be created once via
the CLI:

```bash
# 1. Create the admin token (shown only once — store it as INFLUXDB_TOKEN in .env)
docker compose exec influxdb3-core influxdb3 create token --admin

# 2. Create the raw and downsampled databases
docker compose exec influxdb3-core influxdb3 create database energy_raw --token <token>
docker compose exec influxdb3-core influxdb3 create database energy_downsampled --token <token>

# 3. Create the hourly downsample trigger (community/official downsampler plugin)
docker compose exec influxdb3-core influxdb3 create trigger \
  --database energy_raw \
  --token <token> \
  --path "gh:influxdata/downsampler/downsampler.py" \
  --trigger-spec "every:1h" \
  --trigger-arguments 'source_measurement=energy,target_measurement=energy,interval=1h,window=1h,calculations=avg.min.max,target_database=energy_downsampled'
```

> Note: never pass a real token as a plain CLI argument in scripts that get
> logged or committed — use `--token` interactively or via a short-lived env
> var, not hard-coded.

## Local Development

Run only the infrastructure containers, then run Spring Boot directly from
the IDE/Maven against their published ports:

```bash
docker compose up -d          # InfluxDB 3 Core + Grafana only (backend has profile "full")
```

Secrets that have no working `localhost` default (Fox ESS API key/device SN,
InfluxDB token, Shelly host) are supplied via a **`local` Spring profile**,
not the `.env` file used by Docker Compose — Spring Boot does not read
`.env` files, so values would otherwise never reach the JVM process:

1. Copy `src/main/resources/application-local.yml.example` to
   `application-local.yml` (same directory) and fill in real values
2. `application-local.yml` is gitignored — never commit it
3. Activate the profile:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   (or set `SPRING_PROFILES_ACTIVE=local` / select the `local` profile in
   your IDE's run configuration)

To run the full stack in Docker instead (including the backend container):

```bash
docker compose --profile full up -d --build
```

## Build & Test

```bash
mvn compile          # compile only
mvn test             # run unit/integration tests
mvn package          # build fat JAR → target/energymeter-*.jar
```

## Deployment Target

**Raspberry Pi 4 Model B (ARM64 – linux/arm64)**

Docker images must support the `linux/arm64` platform. Prefer multi-arch
base images (e.g. `eclipse-temurin:21-jre-alpine`, `influxdb:3-core`,
`grafana/grafana`) which publish ARM64 variants automatically.

## Commit Guidelines

- Simplified Conventional Commits, restricted to: `feat`, `fix`, `doc`, `refactor`, `chore`
- Commit message in English: `<type>(optional scope): <description>`
- Add a commit body only for larger/non-obvious changes; skip it for small, self-explanatory commits
- Split work into logically separate commits rather than one large commit
- Always propose the commit (message + file grouping) before it is executed —
  per the global git policy, Claude never runs `git commit` itself; the user
  executes the proposed commit
