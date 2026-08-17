---
title: Pivot
group: Applications
---
# Eclipse Daanse Pivot Application

Pivot is the production Daanse OLAP (XMLA) server. It is built once per supported
database, so every image ships exactly one JDBC DataSource implementation, its
driver and the matching SQL dialect — nothing more.

| Database   | Module     | Container image                          |
|------------|------------|------------------------------------------|
| PostgreSQL | `postgres` | `eclipsedaanse/daanse-pivot-postgres`    |
| MS SQL Server | `mssql` | `eclipsedaanse/daanse-pivot-mssql`       |
| Oracle     | `oracle`   | `eclipsedaanse/daanse-pivot-oracle`      |
| DuckDB     | `duckdb`   | `eclipsedaanse/daanse-pivot-duckdb`      |
| MySQL      | `mysql`    | `eclipsedaanse/daanse-pivot-mysql`       |
| MariaDB    | `mariadb`  | `eclipsedaanse/daanse-pivot-mariadb`     |
| H2         | `h2`       | `eclipsedaanse/daanse-pivot-h2`          |

All images are published with the `:snapshot` tag to Docker Hub
(`docker.io/eclipsedaanse/...`) and the GitHub Container Registry
(`ghcr.io/eclipse-daanse/...`).

## How it works

The `common` module contains the shared runtime description
(`daanse.pivot.base.bndrun`) and a setup service that reads the common
environment variables and wires the server: catalog mapping provider →
context group → XMLA service → XMLA servlet (+ optional CORS filter).

Each database module contributes a small configurator component that reads the
`DAANSE_JDBC_*` environment variables, registers the single DataSource and
creates the OLAP context against it. Every property offered by the underlying
Daanse DataSource bundle is configurable; unset variables fall back to the
bundle defaults.

## Configuration

The server is configured exclusively through environment variables.

Common variables (all images): see the per-image README in each module
(`postgres/README.md`, ...) — they document the shared set
(`DAANSE_CATALOG_RESOURCE`, `DAANSE_XMLA_PATH`, `DAANSE_CORS_*`, ...) plus the
full database specific `DAANSE_JDBC_*` set.

The OLAP catalog (mapping `.xmi` files) is provided via a volume mounted at
`/app/catalog`. A single file is referenced by `DAANSE_CATALOG_RESOURCE`;
additional cross-referenced resources can be included with
`DAANSE_CATALOG_ADDITIONAL_GLOBS`.

## Authentication

By default the XMLA endpoint is anonymous. Every image can authenticate via
HTTP Basic against an LDAP directory (`DAANSE_LDAP_*` variables, roles from
LDAP groups); `DAANSE_AUTH_ANONYMOUS=false` enforces a login. A self contained
example with an LLDAP side container lives in `common/example/ldap`.

## Examples

Self contained demos live in `common/example`: a Docker Compose demo
(`compose/`), the same demo as a Kubernetes pod for Podman/`kubectl`
(`kube/`), and a Compose demo with LDAP authentication (`ldap/`). Each embeds
a seeded database and a minimal catalog in one file.

## Container builds

Each database module carries its image description in `<db>/container/Dockerfile`
and a Testcontainers integration test that builds the image from that
Dockerfile and verifies it end-to-end (XMLA query against a real database).
CI builds every image in a matrix, runs the integration test against the built
image (`-Dpivot.image=...`) and pushes it multi-arch to Docker Hub and GHCR.
`build_images.sh` does the same locally:

```bash
./build_images.sh                 # build all images as daanse-pivot-<db>:local
TAG=dev ./build_images.sh postgres h2   # selected databases, custom tag
PUSH=true IMAGE_PREFIX=docker.io/eclipsedaanse ./build_images.sh
```

## Source Code
The pivot application source can be found at: `https://github.com/eclipse-daanse/org.eclipse.daanse.server/tree/main/application/pivot`
