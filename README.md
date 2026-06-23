# Energy Meter

Home energy monitoring for a Fox ESS Balkonkraftwerk and Shelly power meters,
backed by InfluxDB 3 and visualized in Grafana.

## Stack

- **Backend:** Java 21, Spring Boot 4.1.0, Maven
- **Storage:** InfluxDB 3 Core
- **Dashboards:** Grafana

See [CLAUDE.md](CLAUDE.md) for architecture details and [docs/SETUP.md](docs/SETUP.md)
for the full setup/deployment guide.

## Running in production

```bash
docker compose up -d                     # InfluxDB 3 Core + Grafana
docker compose --profile full up -d      # ...plus the backend (pulls the prebuilt image)
```

Grafana and the backend listen on `127.0.0.1` only. Routing to the outside
(Grafana under `/energymeter`) is handled by the central **nginx reverse
proxy in the `infrastructure` repository** (`../infrastructure`). The backend
itself has no public route — it's a background poller, checked locally via
`curl http://127.0.0.1:8080/api/health`.

## Local development build

To test the production Docker image without a release:

```bash
docker compose -f docker-compose.yml -f docker-compose.local.yml --profile full up --build
```

## Documentation

- [CLAUDE.md](CLAUDE.md) — architecture, conventions, data model
- [docs/SETUP.md](docs/SETUP.md) — prerequisites, local dev, Pi deployment
