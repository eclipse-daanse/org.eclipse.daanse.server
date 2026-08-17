# Daanse Pivot Server — PostgreSQL

Production Daanse OLAP (XMLA) server image for **PostgreSQL**. The image ships
the Daanse ROLAP engine, the XMLA endpoint, the PostgreSQL JDBC driver, the
PostgreSQL DataSource and the matching SQL dialect. It is configured entirely
through environment variables.

Images: `docker.io/eclipsedaanse/daanse-pivot-postgres:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-postgres:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -e DAANSE_JDBC_HOST=db.example.org \
  -e DAANSE_JDBC_DBNAME=warehouse \
  -e DAANSE_JDBC_USER=daanse \
  -e DAANSE_JDBC_PASSWORD=secret \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-postgres:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## PostgreSQL environment variables

Required: `DAANSE_JDBC_DBNAME`, `DAANSE_JDBC_USER`, `DAANSE_JDBC_PASSWORD`.
Unset variables keep the defaults of the Daanse PostgreSQL DataSource bundle.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_HOST` | `localhost` | Database host |
| `DAANSE_JDBC_PORT` | `5432` | Database port (comma separated list for multiple hosts) |
| `DAANSE_JDBC_DBNAME` | *(required)* | Database name |
| `DAANSE_JDBC_USER` | *(required)* | Database user |
| `DAANSE_JDBC_PASSWORD` | *(required)* | Database password |
| `DAANSE_JDBC_CURRENT_SCHEMA` | *(unset)* | Schema search path |
| `DAANSE_JDBC_APPLICATION_NAME` | *(unset)* | Application name reported to the server |
| `DAANSE_JDBC_CONNECT_TIMEOUT` | `0` | Connect timeout (seconds, 0 = infinite) |
| `DAANSE_JDBC_LOGIN_TIMEOUT` | `0` | Login timeout (seconds) |
| `DAANSE_JDBC_SOCKET_TIMEOUT` | `0` | Socket timeout (seconds) |
| `DAANSE_JDBC_SSL` | `false` | Enable SSL |
| `DAANSE_JDBC_SSL_MODE` | *(unset)* | SSL mode (`disable`, `require`, `verify-full`, ...) |
| `DAANSE_JDBC_SSL_CERT` | *(unset)* | Client certificate file |
| `DAANSE_JDBC_SSL_KEY` | *(unset)* | Client key file |
| `DAANSE_JDBC_SSL_ROOT_CERT` | *(unset)* | Root certificate file |
| `DAANSE_JDBC_DEFAULT_ROW_FETCH_SIZE` | `0` | Rows fetched per round trip (0 = all) |
| `DAANSE_JDBC_PREPARE_THRESHOLD` | `5` | Statement executions before server-side prepare |
| `DAANSE_JDBC_PREPARED_STATEMENT_CACHE_QUERIES` | `256` | Prepared statement cache size |
| `DAANSE_JDBC_LOAD_BALANCE_HOSTS` | `false` | Load balance between hosts |
| `DAANSE_JDBC_TARGET_SERVER_TYPE` | *(unset)* | Target server type (`primary`, `secondary`, ...) |
| `DAANSE_JDBC_TCP_KEEP_ALIVE` | `false` | Enable TCP keep-alive |
| `DAANSE_JDBC_READ_ONLY` | `false` | Connection in read-only mode |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/postgres`
