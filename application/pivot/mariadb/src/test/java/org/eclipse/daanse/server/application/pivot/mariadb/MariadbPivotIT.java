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
package org.eclipse.daanse.server.application.pivot.mariadb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.eclipse.daanse.server.application.pivot.common.test.PivotContainers;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

/**
 * End-to-end test: starts a MariaDB database, creates the Fact table of the
 * static test catalog, builds the daanse-pivot-mariadb image from its real
 * Dockerfile, mounts the catalog and verifies via XMLA that the MDX sum over
 * the VALUE column is queryable.
 */
@Testcontainers(disabledWithoutDocker = true)
class MariadbPivotIT {

    static final Network NETWORK = Network.newNetwork();

    @Container
    static final MariaDBContainer<?> DB = new MariaDBContainer<>("mariadb:11.4")
            .withNetwork(NETWORK).withNetworkAliases("db");

    @Test
    void sumMeasureIsQueryableViaXmla() throws Exception {
        try (Connection connection = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
                Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE `Fact` (`KEY` VARCHAR(100), `VALUE` INTEGER)");
            statement.executeUpdate("INSERT INTO `Fact` VALUES ('A', 1), ('B', 2), ('C', 3)");
        }

        try (GenericContainer<?> pivot = new GenericContainer<>(PivotContainers.pivotImage("mariadb"))
                .withNetwork(NETWORK)
                .withCopyFileToContainer(MountableFile.forClasspathResource("catalog.xmi"),
                        "/app/catalog/catalog.xmi")
                .withEnv("DAANSE_JDBC_HOST", "db")
                .withEnv("DAANSE_JDBC_PORT", "3306")
                .withEnv("DAANSE_JDBC_DATABASE_NAME", DB.getDatabaseName())
                .withEnv("DAANSE_JDBC_USER", DB.getUsername())
                .withEnv("DAANSE_JDBC_PASSWORD", DB.getPassword())
                .withExposedPorts(8080)
                .waitingFor(Wait.forListeningPort())) {
            pivot.start();

            String response = PivotContainers.awaitMdxCell(pivot.getHost(), pivot.getMappedPort(8080),
                    PivotContainers.TEST_CATALOG, PivotContainers.TEST_MDX, "6");

            assertTrue(response.contains("6</Value>"), response);
        }
    }
}
