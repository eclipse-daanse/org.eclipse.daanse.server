# Daanse Pivot Server — Oracle

Production Daanse OLAP (XMLA) server image for **Oracle Database**. The image
ships the Daanse ROLAP engine, the XMLA endpoint, the Oracle JDBC driver
(embedded in the DataSource bundle), the DataSource and the matching SQL
dialect. It is configured entirely through environment variables.

Images: `docker.io/eclipsedaanse/daanse-pivot-oracle:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-oracle:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -e DAANSE_JDBC_SERVER_NAME=db.example.org \
  -e DAANSE_JDBC_SERVICE_NAME=freepdb1 \
  -e DAANSE_JDBC_USER=daanse \
  -e DAANSE_JDBC_PASSWORD=secret \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-oracle:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## Oracle environment variables

Required: `DAANSE_JDBC_USER`, `DAANSE_JDBC_PASSWORD` and either
`DAANSE_JDBC_SERVICE_NAME` (service name) or `DAANSE_JDBC_DATABASE_NAME` (SID).
Unset variables keep the defaults of the Daanse Oracle DataSource bundle.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_SERVER_NAME` | `localhost` | Database host |
| `DAANSE_JDBC_PORT_NUMBER` | `1521` | Database port |
| `DAANSE_JDBC_SERVICE_NAME` | *(unset)* | Service name (preferred) |
| `DAANSE_JDBC_DATABASE_NAME` | *(unset)* | SID (alternative to the service name) |
| `DAANSE_JDBC_USER` | *(required)* | Database user |
| `DAANSE_JDBC_PASSWORD` | *(required)* | Database password |
| `DAANSE_JDBC_DEFAULT_ROW_PREFETCH` | `0` | Rows prefetched per round trip (0 = driver default) |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/oracle`
