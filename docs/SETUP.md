# Setup Guide

This guide covers prerequisites, local development, and deployment to the
Raspberry Pi. For architecture and conventions, see [CLAUDE.md](../CLAUDE.md).

## Prerequisites

### Fox ESS Open API (optional)

Only needed if you want to poll the Fox ESS inverter. Skip this section and
set `FOXESS_ENABLED=false` if you only want the Shelly integration for now.

1. In the Fox ESS Cloud app: **Me → API Management** → generate an API key
2. Note your inverter's serial number (shown in the app/on the device)

### Shelly devices

- Shelly devices are polled locally, unauthenticated — no cloud account
  needed, just the device's IP address on your network
- Recommended: assign a static IP or DHCP reservation in your router, so the
  configured host doesn't change after a router reboot

### Tools

| Tool             | Used for                                      |
|------------------|------------------------------------------------|
| Docker + Compose | InfluxDB 3, Grafana, (optionally) the backend |
| Java 21 + Maven  | Running/building the backend locally          |

## Local Development

Run InfluxDB 3 and Grafana in Docker, and the Spring Boot backend directly
from the IDE/Maven — see [CLAUDE.md § Local Development](../CLAUDE.md#local-development)
for the full rationale (`.env` is Compose-only; Spring Boot needs its own
`application-local.yml`).

1. **Copy the secret templates:**
   ```bash
   cp .env.example .env
   cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   ```
   `application-local.yml` only needs Fox ESS values if `foxess.enabled: true`.

2. **Start the infrastructure containers** (InfluxDB 3 Core + Grafana only —
   the backend has Compose profile `full` and is skipped by a plain `up`):
   ```bash
   docker compose up -d
   ```

3. **One-time InfluxDB setup** — admin token, database, and (optionally) the
   downsample trigger. Run these interactively; never paste a real token into
   a script or commit history:
   ```bash
   docker compose exec influxdb3-core influxdb3 create token --admin
   docker compose exec influxdb3-core influxdb3 create database energy_raw --token <token>
   ```
   See [CLAUDE.md § InfluxDB 3 Data Model & Downsampling](../CLAUDE.md) for
   the downsample trigger command (hourly avg/min/max into `energy_downsampled`).

4. **Fill in the real values:**
   - `application-local.yml` → `influxdb.token` (from step 3), `shelly.devices[0].host`,
     and `foxess.api-key`/`device-sn` (if `foxess.enabled: true`)
   - `.env` → `GF_SECURITY_ADMIN_PASSWORD` and `INFLUXDB_TOKEN` (Grafana's
     datasource reads the token from `.env`, not from `application-local.yml`)
   - After changing `.env`, recreate Grafana so it picks up the new value:
     ```bash
     docker compose up -d --force-recreate grafana
     ```

5. **Run the backend:**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
   (or `SPRING_PROFILES_ACTIVE=local` if your shell mangles the `-D` flag)

6. **Verify data is flowing:**
   - Watch the console for `Stored readings for N Shelly device(s)` /
     `Stored N variables for device ...` log lines every `SCHEDULER_INTERVAL_MS`
   - `curl http://localhost:8080/api/health`
   - Query InfluxDB directly: `docker compose exec influxdb3-core influxdb3 query --database energy_raw --token <token> "SELECT * FROM energy ORDER BY time DESC LIMIT 5"`
   - Open Grafana at `http://localhost:3000` → folder **"Energy Meter"**

### Running the full stack in Docker instead

To also containerize the backend (e.g. to test the production image):

```bash
docker compose --profile full up -d --build
```

All backend secrets then come from `.env` instead of `application-local.yml`.

## Deployment to the Raspberry Pi

Target: **Raspberry Pi 4 Model B, ARM64 (`linux/arm64`)**. All images used
(`eclipse-temurin`, `influxdb:3-core`, `grafana/grafana`) are multi-arch, so
the same `docker-compose.yml` works unchanged.

1. Install Docker + Docker Compose on the Pi
2. Copy the project onto the Pi (`git clone` or `rsync`)
3. Create `.env` with **all** real values (Fox ESS, Shelly, InfluxDB token,
   Grafana password) — see `.env.example` for the full list
4. Start everything, including the backend container:
   ```bash
   docker compose --profile full up -d --build
   ```
5. Run the one-time InfluxDB setup (admin token, database, downsample
   trigger) exactly as in local development, step 3 above
6. Update `.env` with the new `INFLUXDB_TOKEN`, then:
   ```bash
   docker compose --profile full up -d --force-recreate
   ```
7. Grafana is reachable at `http://<pi-ip>:3000`

All data is persisted in named Docker volumes (`influxdb3-data`,
`influxdb3-plugins`, `grafana-data`), so it survives container
recreation/Pi reboots.
