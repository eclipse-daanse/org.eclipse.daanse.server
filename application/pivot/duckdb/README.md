# Daanse Pivot Server — DuckDB

Production Daanse OLAP (XMLA) server image for **DuckDB**. The image ships the
Daanse ROLAP engine, the XMLA endpoint, the DuckDB JDBC driver, the DataSource
and the matching SQL dialect. It is configured entirely through environment
variables. The DuckDB database is a file mounted into the container and is
opened read-only by default (an in-memory DuckDB would be private to each
connection and is therefore not useful here).

Note: this image is based on Debian (`eclipse-temurin:25-jre`) instead of
Alpine because the DuckDB native library requires glibc.

Images: `docker.io/eclipsedaanse/daanse-pivot-duckdb:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-duckdb:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -v ./database.duckdb:/app/data/database.duckdb:ro \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-duckdb:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## DuckDB environment variables

There are no required variables — mount the database file at the default
location and the server starts.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_DATABASE_NAME` | `/app/data/database.duckdb` | Path of the DuckDB database file inside the container |
| `DAANSE_JDBC_READ_ONLY` | `true` | Open the database read-only (allows other processes to read the same file); the connection pool inherits this mode |
| `DAANSE_JDBC_SETTINGS` | *(unset)* | Comma separated DuckDB settings, each as `name=value` |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/duckdb`
