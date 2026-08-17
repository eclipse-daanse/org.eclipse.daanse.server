# Daanse Pivot Server — H2

Production Daanse OLAP (XMLA) server image for **H2**. The image ships the
Daanse ROLAP engine, the XMLA endpoint, the H2 JDBC driver, the DataSource and
the matching SQL dialect. It is configured entirely through environment
variables. The H2 database is a file mounted into the container.

Images: `docker.io/eclipsedaanse/daanse-pivot-h2:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-h2:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -v ./database.mv.db:/app/data/database.mv.db \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-h2:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## H2 environment variables

There are no required variables — mount the database file at the default
location and the server starts. The database identifier is the file path
without the `.mv.db` suffix.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_IDENTIFIER` | `/app/data/database` | Database identifier (path for the `file` filesystem) |
| `DAANSE_JDBC_PLUGABLE_FILESYSTEM` | `file` | H2 pluggable filesystem (`file`, `zip`, `nioMapped`, `async`, `memFS`, `memLZF`, `nioMemFS`, `nioMemLZF`) |
| `DAANSE_JDBC_USERNAME` | *(unset)* | Database user (note: derived from the `username` attribute, not `user`) |
| `DAANSE_JDBC_PASSWORD` | *(unset)* | Database password |
| `DAANSE_JDBC_DATABASE_TO_UPPER` | `true` | Fold unquoted identifiers to upper case |
| `DAANSE_JDBC_DB_CLOSE_DELAY` | `0` | Keep the database open after the last connection closes (seconds, -1 = until VM exit) |
| `DAANSE_JDBC_DEBUG` | `false` | H2 trace output |
| `DAANSE_JDBC_DESCRIPTION` | *(unset)* | Description of the DataSource |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/h2`
