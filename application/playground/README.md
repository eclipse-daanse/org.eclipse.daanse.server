---
title: Playground
group: Applications
---
<!-- markdownlint-disable-next-line MD025 -->
# Eclipse Daanse Playground Application

**Availability: Git Repository Only - No Docker Container/Artifact**

The playground application is a comprehensive development tool that includes all components necessary for creating and configuring Daanse OLAP servers. It serves as a complete testing and development environment.

## What's Included

### Complete Component Set
- **All Dialects**: Support for every database dialect available in the Daanse ecosystem
- **All Data Sources**: Every possible data source connector included
- **Web Consoles**: Administrative and management web interfaces
- **Helper Components**: Tools that assist in creating and configuring pivot servers

### Development Features
- **Full Configuration Testing**: Test all possible configurations before production deployment
- **Component Exploration**: Discover and experiment with different Daanse components
- **Integration Testing**: Validate component interactions in a complete environment

## Security Considerations

### Why No Docker Release
Due to the large number of components included in the playground application, the attack surface is significantly increased. For this reason, the Daanse project has decided not to provide a Docker release or pre-built artifacts.

### Risk Assessment
- **Increased Attack Surface**: More components mean more potential security vulnerabilities
- **Development Only**: Not intended for production environments
- **Security Trade-off**: Comprehensive functionality vs. security exposure

## Use Cases

### Development Tool
- **Server Customization**: Ideal for adapting and developing Daanse OLAP servers
- **Component Selection**: Help determine which components are needed for specific use cases
- **Configuration Validation**: Test configurations before implementing in production

### Learning Environment
- **Feature Exploration**: Understand all available Daanse capabilities
- **Component Dependencies**: Learn how different components interact
- **Best Practices**: Develop understanding of optimal configurations

## Getting Started

### Source Code Location
The playground application source can be found at:
`https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/playground`

### Prerequisites
- Access to the Daanse git repository
- Java development environment
- Understanding of security implications

## Default configuration

The `load/` directory is watched by Felix FileInstall (its default directory) and ships a
working three-database default: one catalog served from **PostgreSQL**, one from
**Oracle** and one from an embedded **DuckDB**, all published on the **same XMLA endpoint**.

| File(s) | Purpose |
| --- | --- |
| `daanse.jdbc.datasource.{postgresql,oracle,duckdb}.DataSource-*.cfg` | one DataSource per database, tagged `daanse.ident=ds-pg` / `ds-ora` / `ds-duck` |
| `daanse.jdbc.datasource.pools.hikari.ConnectionPool-*.cfg` | one connection pool per DataSource |
| `daanse.rolap.mapping.model.provider.EmfMappingProvider-*.cfg` | one mapping per catalog XMI (`catalog/catalog.{postgres,oracle,duckdb}.xmi`) |
| `daanse.rolap.core.BasicContext-*.cfg` | one context per catalog: pool + mapping + dialect (`POSTGRESQL` / `ORACLE` / `DUCKDB`) |
| `daanse.olap.core.BasicContextGroup-main.cfg` | groups the contexts (`context.target=(daanse.ident=ctx-*)`) |
| `daanse.olap.xmla.connector.OlapXmlaConnector-main.cfg` | XMLA service over the group |
| `org.eclipse.daanse.xmla.server.whiteboard.servlet.XmlaServlet-main.cfg` | servlet at `/xmla` |

The endpoint is `http://localhost:8090/xmla` (HTTP port from the bndrun). The catalogs —
`Playground Postgres` (cube `CubePostgres`), `Playground Oracle` (cube `CubeOracle`) and
`Playground DuckDB` (cube `CubeDuckDB`) — appear in `DBSCHEMA_CATALOGS` of that one
endpoint; each expects a table `fact("key","value")` / `FACT("KEY","VALUE")`. A catalog
whose database is not reachable is left out; the others are served regardless.

### DuckDB

DuckDB runs inside the server process, so its catalog works without any external database.
The DataSource opens an in-memory database (`databaseName = ":memory:"`) and runs
`catalog/catalog.duckdb.sql` once per database instance (`sessionInitSqlFile`), which
creates and fills the `fact` table. To query a DuckDB file of your own instead, point
`databaseName` at it in `load/daanse.jdbc.datasource.duckdb.DataSource-duck.cfg` and drop
`sessionInitSqlFile`; add `readOnly = B"true"` to let other processes read the same file
while the server runs (the pool then has to be read-only as well, `readOnly = B"true"` in
`load/daanse.jdbc.datasource.pools.hikari.ConnectionPool-duck.cfg`).

Matching local databases:

```bash
podman run -d --name playground-pg -e POSTGRES_USER=daanse -e POSTGRES_PASSWORD=daanse \
  -e POSTGRES_DB=daanse -p 5432:5432 docker.io/library/postgres:17-alpine
podman run -d --name playground-ora -e ORACLE_PASSWORD=admin -e APP_USER=daanse \
  -e APP_USER_PASSWORD=daanse -p 1521:1521 docker.io/gvenzl/oracle-free:23-slim
```

Then create/seed the fact tables (`fact` lowercase in PostgreSQL, `FACT` uppercase in
Oracle service `FREEPDB1`) and start the server with `./start` (or
`java -jar target/daanse.playground.jar` from this directory — FileInstall resolves
`./load` and the mappings resolve `./catalog` relative to the working directory).

## Important Notes

⚠️ **Security Warning**: This application should only be used in development environments due to the increased attack surface from the comprehensive component set.

✅ **Development Value**: Despite security considerations, this is a very helpful tool for customizing and developing Daanse OLAP servers.

🔧 **Configuration Helper**: Essential for understanding component relationships and optimal server configurations before production deployment.