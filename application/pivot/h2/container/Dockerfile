FROM eclipse-temurin:25-jre-alpine

# Configurable UID/GID to avoid permission problems with bind mounts (e.g. rootless Podman)
ARG APP_UID=1000
ARG APP_GID=1000

# Create non-root user for security
RUN addgroup -S -g ${APP_GID} appgroup \
 && adduser  -S -u ${APP_UID} -G appgroup appuser

LABEL org.opencontainers.image.source="https://github.com/eclipse-daanse/org.eclipse.daanse.server" \
      org.opencontainers.image.licenses="EPL-2.0" \
      org.opencontainers.image.vendor="Eclipse Daanse" \
      org.opencontainers.image.description="Daanse OLAP (XMLA) server for H2"

WORKDIR /app

# Copy application files
COPY --chown=appuser:appgroup application/pivot/h2/target/daanse.pivot.h2.jar /app/
COPY --chown=appuser:appgroup application/pivot/h2/start /app/
COPY --chown=appuser:appgroup application/pivot/h2/logback.xml /app/

RUN mkdir -p /app/catalog /app/data /app/log \
 && chown -R appuser:appgroup /app \
 && chmod +x /app/start

USER appuser

VOLUME ["/app/catalog"]

EXPOSE 8080

ENTRYPOINT ["sh","-c","exec /app/start"]
