# Daanse Pivot Server — MS SQL Server

Production Daanse OLAP (XMLA) server image for **Microsoft SQL Server**. The
image ships the Daanse ROLAP engine, the XMLA endpoint, the MS SQL Server JDBC
driver, the DataSource and the matching SQL dialect. It is configured entirely
through environment variables.

Images: `docker.io/eclipsedaanse/daanse-pivot-mssql:snapshot` and
`ghcr.io/eclipse-daanse/daanse-pivot-mssql:snapshot` (linux/amd64, linux/arm64).

## Quick start

```bash
docker run --name daanse-pivot \
  -v ./catalog:/app/catalog:ro \
  -e DAANSE_JDBC_SERVER_NAME=db.example.org \
  -e DAANSE_JDBC_DATABASE_NAME=warehouse \
  -e DAANSE_JDBC_USER=daanse \
  -e DAANSE_JDBC_PASSWORD=secret \
  -p 8080:8080 \
  eclipsedaanse/daanse-pivot-mssql:snapshot
```

The XMLA endpoint is then available at `http://localhost:8080/xmla`.

## MS SQL Server environment variables

Required: `DAANSE_JDBC_DATABASE_NAME`, `DAANSE_JDBC_USER`, `DAANSE_JDBC_PASSWORD`.
Unset variables keep the defaults of the Daanse MS SQL Server DataSource bundle.

| Variable | Default | Description |
|---|---|---|
| `DAANSE_JDBC_SERVER_NAME` | `localhost` | Database host |
| `DAANSE_JDBC_PORT_NUMBER` | `1433` | Database port |
| `DAANSE_JDBC_INSTANCE_NAME` | *(unset)* | Named instance |
| `DAANSE_JDBC_DATABASE_NAME` | *(required)* | Database name |
| `DAANSE_JDBC_USER` | *(required)* | Database user |
| `DAANSE_JDBC_PASSWORD` | *(required)* | Database password |
| `DAANSE_JDBC_ENCRYPT` | `true` | Encrypt the connection (`true`, `false`, `strict`) |
| `DAANSE_JDBC_TRUST_SERVER_CERTIFICATE` | `false` | Trust the server certificate without validation |
| `DAANSE_JDBC_INTEGRATED_SECURITY` | `false` | Use integrated security |
| `DAANSE_JDBC_AUTHENTICATION` | `NotSpecified` | Authentication method |
| `DAANSE_JDBC_APPLICATION_NAME` | *(unset)* | Application name reported to the server |
| `DAANSE_JDBC_APPLICATION_INTENT` | `readwrite` | `readwrite` or `readonly` |
| `DAANSE_JDBC_LOGIN_TIMEOUT` | `30` | Login timeout (seconds) |
| `DAANSE_JDBC_QUERY_TIMEOUT` | `-1` | Query timeout (seconds, -1 = unlimited) |
| `DAANSE_JDBC_SOCKET_TIMEOUT` | `0` | Socket timeout (milliseconds, 0 = unlimited) |
| `DAANSE_JDBC_LOCK_TIMEOUT` | `-1` | Lock timeout (milliseconds, -1 = server default) |
| `DAANSE_JDBC_RESPONSE_BUFFERING` | `adaptive` | `adaptive` or `full` |
| `DAANSE_JDBC_SELECT_METHOD` | `direct` | `direct` or `cursor` |
| `DAANSE_JDBC_MULTI_SUBNET_FAILOVER` | `false` | Multi-subnet failover |
| `DAANSE_JDBC_FAILOVER_PARTNER` | *(unset)* | Failover partner server |
| `DAANSE_JDBC_PACKET_SIZE` | `4096` | Network packet size |
| `DAANSE_JDBC_COLUMN_ENCRYPTION_SETTING` | `Disabled` | Always Encrypted setting |
| `DAANSE_JDBC_SEND_STRING_PARAMETERS_AS_UNICODE` | `true` | Send strings as Unicode |

## Source

`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot/mssql`
