# DuckDB JDBC ships glibc natives — musl/Alpine is not supported
FROM eclipse-temurin:25-jre

# Configurable UID/GID to avoid permission problems with bind mounts (e.g. rootless Podman)
ARG APP_UID=1000
ARG APP_GID=1000

# Create non-root user for security. The Ubuntu based image may already ship a
# user with this UID — reuse it in that case and address it numerically below.
RUN getent group ${APP_GID} >/dev/null || groupadd --system --gid ${APP_GID} appgroup; \
    getent passwd ${APP_UID} >/dev/null || useradd --system --uid ${APP_UID} --gid ${APP_GID} appuser

LABEL org.opencontainers.image.source="https://github.com/eclipse-daanse/org.eclipse.daanse.server" \
      org.opencontainers.image.licenses="EPL-2.0" \
      org.opencontainers.image.vendor="Eclipse Daanse" \
      org.opencontainers.image.description="Daanse OLAP (XMLA) server for DuckDB"

WORKDIR /app

# Copy application files
COPY --chown=${APP_UID}:${APP_GID} application/pivot/duckdb/target/daanse.pivot.duckdb.jar /app/
COPY --chown=${APP_UID}:${APP_GID} application/pivot/duckdb/start /app/
COPY --chown=${APP_UID}:${APP_GID} application/pivot/duckdb/logback.xml /app/

RUN mkdir -p /app/catalog /app/data /app/log \
 && chown -R ${APP_UID}:${APP_GID} /app \
 && chmod +x /app/start

USER ${APP_UID}:${APP_GID}

VOLUME ["/app/catalog"]

EXPOSE 8080

ENTRYPOINT ["sh","-c","exec /app/start"]
