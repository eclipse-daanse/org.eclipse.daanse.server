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

FROM eclipse-temurin:21-jre-alpine

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy application files
COPY --chown=appuser:appgroup application/probe/target/daanse.probe.jar /app/
COPY --chown=appuser:appgroup application/probe/start /app/
COPY --chown=appuser:appgroup application/probe/logback.xml /app/
COPY --chown=appuser:appgroup application/probe/catalog/ /app/catalog/
COPY --chown=appuser:appgroup application/probe/log/ /app/log/

# Make start script executable
RUN chmod +x /app/start

# Switch to non-root user
USER appuser

# Expose common application port (adjust as needed)
EXPOSE 8080

# Use shell script as entrypoint
ENTRYPOINT ["sh","/app/start"]