#*******************************************************************************
# Copyright (c)  2025 Contributors to the Eclipse Foundation
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
#   Contributors:
#   SmartCity Jena - initial
#   Stefan Bischof (bipolis.org) - initial
#*******************************************************************************

# DuckDB JDBC ships glibc natives — musl/Alpine is not supported
FROM eclipse-temurin:25-jre

# Set UID/GID → less Problems with Bind-Mounts (Podman rootless)
ARG APP_UID=1000
ARG APP_GID=1000

# Create non-root user for security. The Ubuntu based image may already ship a
# user with this UID — reuse it in that case and address it numerically below.
RUN getent group ${APP_GID} >/dev/null || groupadd --system --gid ${APP_GID} appgroup; \
    getent passwd ${APP_UID} >/dev/null || useradd --system --uid ${APP_UID} --gid ${APP_GID} appuser

LABEL org.opencontainers.image.source="https://github.com/eclipse-daanse/org.eclipse.daanse.server" \
      org.opencontainers.image.licenses="EPL-2.0" \
      org.opencontainers.image.vendor="Eclipse Daanse" \
      org.opencontainers.image.description="Daanse Probe - OLAP catalog development and testing server"

WORKDIR /app

# Copy application files
COPY --chown=${APP_UID}:${APP_GID} application/probe/target/daanse.probe.jar /app/
COPY --chown=${APP_UID}:${APP_GID} application/probe/start /app/
COPY --chown=${APP_UID}:${APP_GID} application/probe/logback.xml /app/
COPY --chown=${APP_UID}:${APP_GID} application/probe/catalog/ /app/catalog/
COPY --chown=${APP_UID}:${APP_GID} application/probe/output/ /app/output/
COPY --chown=${APP_UID}:${APP_GID} application/probe/log/ /app/log/

# Make start script executable
RUN chmod +x /app/start

# Switch to non-root user
USER ${APP_UID}:${APP_GID}

# XMLA endpoint (/xmla) on the single Jetty HTTP server
EXPOSE 8080

# Use shell script as entrypoint
ENTRYPOINT ["sh","-c","exec /app/start"]
