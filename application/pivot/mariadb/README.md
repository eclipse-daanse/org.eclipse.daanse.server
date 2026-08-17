# Daanse Pivot Server — MariaDB

Production Daanse OLAP (XMLA) server image for **MariaDB**. The image ships the
Daanse ROLAP engine, the XMLA endpoint, the MariaDB JDBC driver, the DataSource
and the matching SQL dialect. It is configured entirely through environment
variables.

Images: `docker.io/eclipsedaanse/daanse-pivot-mariadb:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-mariadb:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -e DAANSE_JDBC_HOST=db.example.org \
  -e DAANSE_JDBC_DATABASE_NAME=warehouse \
  -e DAANSE_JDBC_USER=daanse \
  -e DAANSE_JDBC_PASSWORD=secret \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-mariadb:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## MariaDB environment variables

Required: `DAANSE_JDBC_DATABASE_NAME`, `DAANSE_JDBC_USER`, `DAANSE_JDBC_PASSWORD`.
Unset variables keep the defaults of the Daanse MariaDB DataSource bundle.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_HOST` | `localhost` | Database host |
| `DAANSE_JDBC_PORT` | `3306` | Database port |
| `DAANSE_JDBC_DATABASE_NAME` | *(required)* | Database name |
| `DAANSE_JDBC_USER` | *(required)* | Database user |
| `DAANSE_JDBC_PASSWORD` | *(required)* | Database password |
| `DAANSE_JDBC_CONNECT_TIMEOUT` | `30000` | Connect timeout (milliseconds) |
| `DAANSE_JDBC_SOCKET_TIMEOUT` | `0` | Socket timeout (milliseconds, 0 = unlimited) |
| `DAANSE_JDBC_SSL_MODE` | `disable` | SSL mode (`disable`, `trust`, `verify-ca`, `verify-full`) |
| `DAANSE_JDBC_USE_SERVER_PREP_STMTS` | `false` | Server side prepared statements |
| `DAANSE_JDBC_ALLOW_LOCAL_INFILE` | `true` | Allow LOAD DATA LOCAL INFILE |
| `DAANSE_JDBC_USE_COMPRESSION` | `false` | Compress the connection |
| `DAANSE_JDBC_ALLOW_MULTI_QUERIES` | `false` | Allow multiple queries per statement |
| `DAANSE_JDBC_SESSION_VARIABLES` | *(unset)* | Session variables set on connect |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/mariadb`
