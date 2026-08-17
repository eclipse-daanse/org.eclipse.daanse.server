/*
* Copyright (c) 2026 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   SmartCity Jena - initial
*   Stefan Bischof (bipolis.org) - initial
*/
package org.eclipse.daanse.server.application.pivot.h2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.eclipse.daanse.server.application.pivot.common.test.PivotContainers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * End-to-end test: creates an H2 database file with the Fact table of the
 * static test catalog, builds the daanse-pivot-h2 image from its real
 * Dockerfile, mounts database file and catalog and verifies via XMLA that the
 * MDX sum over the VALUE column is queryable.
 */
@Testcontainers(disabledWithoutDocker = true)
class H2PivotIT {

    @TempDir
    static Path tempDir;

    @Test
    void sumMeasureIsQueryableViaXmla() throws Exception {
        Path databaseBase = tempDir.resolve("database");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:" + databaseBase);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE \"Fact\" (\"KEY\" VARCHAR(100), \"VALUE\" INTEGER)");
            statement.executeUpdate("INSERT INTO \"Fact\" VALUES ('A', 1), ('B', 2), ('C', 3)");
        }
        Path databaseFile = tempDir.resolve("database.mv.db");
        assertTrue(Files.exists(databaseFile));

        try (GenericContainer<?> pivot = new GenericContainer<>(PivotContainers.pivotImage("h2"))
                .withCopyFileToContainer(MountableFile.forClasspathResource("catalog.xmi"),
                        "/app/catalog/catalog.xmi")
                .withCopyFileToContainer(MountableFile.forHostPath(databaseFile), "/app/data/database.mv.db")
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPort())) {
            pivot.start();

            String response = PivotContainers.awaitMdxCell(pivot.getHost(), pivot.getMappedPort(8080),
                    PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, "6");

            assertTrue(response.contains("6</Value>"), response);
        }
    }
}
