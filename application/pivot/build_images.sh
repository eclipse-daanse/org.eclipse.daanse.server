#!/usr/bin/env bash
#*******************************************************************************
# Copyright (c)  2026 Contributors to the Eclipse Foundation
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#*******************************************************************************
#
# Builds the pivot server jars and container images and verifies each image
# with its Testcontainers integration test. The tests run against exactly the
# image built here (-Dpivot.image), the same flow the CI pipeline uses.
#
# Usage: ./build_images.sh [db ...]
#   db            one or more of: postgres mssql oracle duckdb mysql mariadb h2
#                 (default: all)
#
# Environment:
#   TAG           image tag (default: local)
#   PUSH          set to "true" to push the verified images
#   IMAGE_PREFIX  registry/namespace for pushed images
#                 (default: docker.io/eclipsedaanse)
set -euo pipefail

ALL_DBS=(postgres mssql oracle duckdb mysql mariadb h2)
DBS=("$@")
if [ ${#DBS[@]} -eq 0 ]; then
  DBS=("${ALL_DBS[@]}")
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TAG="${TAG:-local}"
PUSH="${PUSH:-false}"
IMAGE_PREFIX="${IMAGE_PREFIX:-docker.io/eclipsedaanse}"

# The build requires JDK 25 (bndruns target JavaSE-25).
JAVA_MAJOR="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -1)"
if [ "${JAVA_MAJOR:-0}" -lt 25 ]; then
  echo "ERROR: JDK 25+ required, found ${JAVA_MAJOR:-none}. Set JAVA_HOME accordingly." >&2
  exit 1
fi

# Testcontainers may be configured to a different container daemon than the
# docker CLI default (docker.host in ~/.testcontainers.properties, e.g. a
# rootless Podman socket). Build against the same daemon so the verification
# tests find the image.
TC_PROPS="$HOME/.testcontainers.properties"
if [ -z "${DOCKER_HOST:-}" ] && [ -f "$TC_PROPS" ]; then
  TC_HOST="$(sed -nE 's/^docker\.host=(.+)$/\1/p' "$TC_PROPS" | head -1)"
  TC_HOST="${TC_HOST//\\/}"
  if [ -n "$TC_HOST" ]; then
    export DOCKER_HOST="$TC_HOST"
    echo "==> Using container daemon from ${TC_PROPS}: ${DOCKER_HOST}"
  fi
fi

MODULES="application/pivot/common"
for db in "${DBS[@]}"; do
  MODULES="${MODULES},application/pivot/${db}"
done

cd "$REPO_ROOT"

echo "==> Building jars for: ${DBS[*]}"
mvn -B -am -pl "$MODULES" package

for db in "${DBS[@]}"; do
  echo "==> Building image daanse-pivot-${db}:${TAG}"
  # --load makes buildx builders load the result into the image store
  docker build --load -f "application/pivot/${db}/container/Dockerfile" \
    -t "daanse-pivot-${db}:${TAG}" "$REPO_ROOT"
done

for db in "${DBS[@]}"; do
  echo "==> Verifying daanse-pivot-${db}:${TAG} with the integration test"
  mvn -B -pl "application/pivot/${db}" \
    failsafe:integration-test failsafe:verify \
    "-Dpivot.image=daanse-pivot-${db}:${TAG}"
done

if [ "$PUSH" = "true" ]; then
  for db in "${DBS[@]}"; do
    image="${IMAGE_PREFIX}/daanse-pivot-${db}:${TAG}"
    echo "==> Pushing ${image}"
    docker tag "daanse-pivot-${db}:${TAG}" "$image"
    docker push "$image"
  done
fi

echo "==> Done: ${DBS[*]} (tag: ${TAG})"
