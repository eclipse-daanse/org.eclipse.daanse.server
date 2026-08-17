# Daanse Pivot Server — MySQL

Production Daanse OLAP (XMLA) server image for **MySQL**. The image ships the
Daanse ROLAP engine, the XMLA endpoint, the MySQL JDBC driver, the DataSource
and the matching SQL dialect. It is configured entirely through environment
variables.

Images: `docker.io/eclipsedaanse/daanse-pivot-mysql:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-mysql:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -e DAANSE_JDBC_HOST=db.example.org \
  -e DAANSE_JDBC_DATABASE_NAME=warehouse \
  -e DAANSE_JDBC_USER=daanse \
  -e DAANSE_JDBC_PASSWORD=secret \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-mysql:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## MySQL environment variables

Required: `DAANSE_JDBC_DATABASE_NAME`, `DAANSE_JDBC_USER`, `DAANSE_JDBC_PASSWORD`.
Unset variables keep the defaults of the Daanse MySQL DataSource bundle.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_HOST` | `localhost` | Database host |
| `DAANSE_JDBC_PORT` | `3306` | Database port |
| `DAANSE_JDBC_DATABASE_NAME` | *(required)* | Database name |
| `DAANSE_JDBC_USER` | *(required)* | Database user |
| `DAANSE_JDBC_PASSWORD` | *(required)* | Database password |
| `DAANSE_JDBC_AUTO_RECONNECT` | `false` | Auto reconnect |
| `DAANSE_JDBC_CONNECT_TIMEOUT` | `0` | Connect timeout (milliseconds, 0 = unlimited) |
| `DAANSE_JDBC_SOCKET_TIMEOUT` | `0` | Socket timeout (milliseconds, 0 = unlimited) |
| `DAANSE_JDBC_USE_SSL` | `false` | Use SSL |
| `DAANSE_JDBC_CHARACTER_ENCODING` | `UTF-8` | Character encoding |
| `DAANSE_JDBC_USE_SERVER_PREP_STMTS` | `false` | Server side prepared statements |
| `DAANSE_JDBC_CACHE_PREP_STMTS` | `false` | Cache prepared statements |
| `DAANSE_JDBC_PREP_STMT_CACHE_SIZE` | `25` | Prepared statement cache size |
| `DAANSE_JDBC_USE_COMPRESSION` | `false` | Compress the connection |
| `DAANSE_JDBC_ZERO_DATE_TIME_BEHAVIOR` | `exception` | Behavior for zero datetime values |
| `DAANSE_JDBC_SERVER_TIMEZONE` | *(unset)* | Server timezone override |
| `DAANSE_JDBC_USE_AFFECTED_ROWS` | `false` | Report affected instead of found rows |
| `DAANSE_JDBC_ALLOW_MULTI_QUERIES` | `false` | Allow multiple queries per statement |
| `DAANSE_JDBC_REWRITE_BATCHED_STATEMENTS` | `false` | Rewrite batched statements |
| `DAANSE_JDBC_USE_INFORMATION_SCHEMA` | `true` | Use INFORMATION_SCHEMA for metadata |
| `DAANSE_JDBC_LOGGER` | *(unset)* | Driver logger class |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/mysql`
